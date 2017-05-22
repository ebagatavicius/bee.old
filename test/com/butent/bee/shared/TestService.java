package com.butent.bee.shared;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link com.butent.bee.shared.Service}.
 */
@SuppressWarnings("static-method")
public class TestService {

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public final void testIsDataService() {
    assertTrue(Service.isDataService("rpc_data_"));
    assertTrue(Service.isDataService("rpc_data_complex"));
    assertTrue(Service.isDataService("rpc_data_win"));

    assertFalse(Service.isDataService("\t\t\t\r rpc_data_"));
    assertFalse(Service.isDataService("rpc_"));
    assertFalse(Service.isDataService("rpc_dataada"));
    assertFalse(Service.isDataService("arpc_data_"));
    assertFalse(Service.isDataService("a rpc_data_"));
  }

  @Test
  public final void testIsDbMetaService() {
    assertTrue(Service.isDbMetaService("rpc_db_meta_"));
    assertTrue(Service.isDbMetaService("rpc_db_meta__complex"));
    assertTrue(Service.isDbMetaService("rpc_db_meta_win"));

    assertFalse(Service.isDbMetaService("\t\t\t\r rpc_db_meta_"));
    assertFalse(Service.isDbMetaService("rpc_"));
    assertFalse(Service.isDbMetaService("rpc_db_metaetabeta"));
    assertFalse(Service.isDbMetaService("arpc_db_meta_"));
    assertFalse(Service.isDbMetaService("a rpc_db_meta_"));
  }

  @Test
  public final void testIsDbService() {
    assertTrue(Service.isDbService("rpc_db_"));
    assertTrue(Service.isDbService("rpc_db__complex"));
    assertTrue(Service.isDbService("rpc_db_win"));

    assertFalse(Service.isDbService("\t\t\t\r rpc_db_"));
    assertFalse(Service.isDbService("rpc_"));
    assertFalse(Service.isDbService("rpc_dbmetaetabeta"));
    assertFalse(Service.isDbService("arpc_db_"));
    assertFalse(Service.isDbService("a rpc_db_"));
  }

  @Test
  public final void testIsInvocation() {

    assertFalse(Service.isInvocation(""));

    assertFalse(Service.isInvocation("\t \r \r"));

    assertFalse(Service.isInvocation(null));

    assertTrue(Service.isInvocation("rpc_invoke"));
    assertTrue(Service.isInvocation("rpc_iNvoke"));
    assertTrue(Service.isInvocation("rpc_invoke           "));
    assertTrue(Service.isInvocation("             rpc_invoke"));
    assertTrue(Service.isInvocation("\0\0\0 rpc_invoke"));
    assertTrue(Service.isInvocation("rpc_INVoke "));

    assertFalse(Service.isInvocation("\t\t\t\r rpc_invokre"));
    assertFalse(Service.isInvocation("rpc_"));
    assertFalse(Service.isInvocation("r\0pc_invokess"));
    assertFalse(Service.isInvocation("arpc_inv\0oke"));
    assertFalse(Service.isInvocation("a rpc_inveoke"));
  }

  @Test
  public final void testIsSysService() {
    assertTrue(Service.isSysService("rpc_sys_"));
    assertTrue(Service.isSysService("rpc_sys__complex"));
    assertTrue(Service.isSysService("rpc_sys_win"));
    assertTrue(Service.isSysService("rpc_sys_"));

    assertFalse(Service.isSysService("\t\t\t\r rpc_sys_"));
    assertFalse(Service.isSysService("rpc"));
    assertFalse(Service.isSysService("rpc_sysdbmetaetabeta"));
    assertFalse(Service.isSysService("arpc_sys_"));
    assertFalse(Service.isSysService("a rpc_sys_"));
  }
}
