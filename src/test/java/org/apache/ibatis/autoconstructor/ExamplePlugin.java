package org.apache.ibatis.autoconstructor;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandlerRegistry;

import java.sql.Statement;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Matcher;

@Intercepts({@Signature(
  type= StatementHandler.class,
  method = "query",
  args = {Statement.class, ResultHandler.class})})
public class ExamplePlugin implements Interceptor {
  private Properties properties;

  @Override
  public Object intercept(Invocation invocation) throws Throwable {
    // implement pre processing if need
    MetaObject metaObject = SystemMetaObject.forObject(invocation.getTarget());
    Configuration configuration = (Configuration) metaObject.getValue("delegate.configuration");
    BoundSql boundSql = (BoundSql) metaObject.getValue("delegate.boundSql");
    String parameterizedSql = this.getParameterizedSql(configuration, boundSql);
    System.out.println("完整sql语句：" + parameterizedSql);
    Object returnObject = invocation.proceed();
    // implement post processing if need
    return returnObject;
  }

  // 进行？的替换
  public String getParameterizedSql(Configuration configuration, BoundSql boundSql) {
    // 获取参数
    Object parameterObject = boundSql.getParameterObject();
    List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
    // sql语句中多个空格都用一个空格代替
    String sql = boundSql.getSql().replaceAll("[\\s]+", " ");
    if (parameterMappings != null && !parameterMappings.isEmpty() && parameterObject != null) {
      // 获取类型处理器注册器，类型处理器的功能是进行java类型和数据库类型的转换
      TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
      // 如果根据parameterObject.getClass(）可以找到对应的类型，则替换
      if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
        sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(getParameterValue(parameterObject)));
      } else {
        // MetaObject主要是封装了originalObject对象，提供了get和set的方法用于获取和设置originalObject的属性值,主要支持对JavaBean、Collection、Map三种类型对象的操作
        MetaObject metaObject = configuration.newMetaObject(parameterObject);
        for (ParameterMapping parameterMapping : parameterMappings) {
          String propertyName = parameterMapping.getProperty();
          if (metaObject.hasGetter(propertyName)) {
            Object obj = metaObject.getValue(propertyName);
            sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(getParameterValue(obj)));
          } else if (boundSql.hasAdditionalParameter(propertyName)) {
            // 该分支是动态sql
            Object obj = boundSql.getAdditionalParameter(propertyName);
            sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(getParameterValue(obj)));
          } else {
            // 打印出缺失，提醒该参数缺失并防止错位
            sql = sql.replaceFirst("\\?", "缺失");
          }
        }
      }
    }
    return sql;
  }

  // 如果参数是String，则添加单引号， 如果是日期，则转换为时间格式器并加单引号； 对参数是null和不是null的情况作了处理
  private static String getParameterValue(Object obj) {
    String value = null;
    if (obj instanceof String) {
      value = "'" + obj.toString() + "'";
    } else if (obj instanceof Date) {
      DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.CHINA);
      value = "'" + formatter.format(new Date()) + "'";
    } else {
      if (obj != null) {
        value = obj.toString();
      } else {
        value = "";
      }

    }
    return value;
  }

  @Override
  public Object plugin(Object target) {
    return Plugin.wrap(target, this);
  }

  @Override
  public void setProperties(Properties properties) {
    this.properties = properties;
  }
}
