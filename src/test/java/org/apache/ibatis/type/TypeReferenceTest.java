package org.apache.ibatis.type;

/**
 * @Author: xxn
 * @Date: Created in 2020/07/30 9:17
 */
public class TypeReferenceTest {

  public static void main(String[] args) {
    new TypeReferenceDemo();
  }

  private static class TypeReferenceDemo extends TypeReferenceDemo1 {
  }

  private static class TypeReferenceDemo1 extends TypeReference<TypeReference> {
    private String type;

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }
  }

}
