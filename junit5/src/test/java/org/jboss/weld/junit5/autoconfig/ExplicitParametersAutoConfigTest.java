package org.jboss.weld.junit5.autoconfig;

import javax.enterprise.inject.Default;

import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.ExplicitParamInjection;
import org.jboss.weld.junit5.basic.Foo;
import org.jboss.weld.junit5.explicitInjection.Bar;
import org.jboss.weld.junit5.explicitInjection.CustomExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;



@EnableWeld
@ExplicitParamInjection
@ExtendWith(CustomExtension.class)
public class ExplicitParametersAutoConfigTest {

  @Test
  @DisplayName("Ensure the parameter Foo is automatically included in container and that Bar comes from the CustomExtension")
  void test(@Default Foo foo, Bar bar) {
    assertNotNull(bar);
    assertEquals(bar.ping(), CustomExtension.class.getSimpleName());
    assertNotNull(foo);
    assertEquals(foo.getBar(), "baz");
  }

}
