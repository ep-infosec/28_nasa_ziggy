package gov.nasa.ziggy.services.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link Role} class.
 *
 * @author Bill Wohler
 */
public class RoleTest {
    private Role role;

    @Before
    public void setUp() {
        role = new Role("manager");
    }

    @Test
    public void testConstructors() {
        Role role = new Role("operator");
        assertEquals("operator", role.getName());

        User user = new User("bar", "Bar", "foo@bar", "x4242");
        role = new Role("foo", user);
        assertEquals("foo", role.getName());
        assertEquals(user, role.getCreatedBy());
    }

    @Test
    public void testPrivileges() {
        assertEquals(0, role.getPrivileges().size());

        List<String> pList = new LinkedList<>();
        pList.add(Privilege.PIPELINE_MONITOR.toString());
        role.setPrivileges(pList);
        assertEquals(Privilege.PIPELINE_MONITOR.toString(), role.getPrivileges().get(0));

        role.addPrivilege(Privilege.PIPELINE_MONITOR.toString());
        assertEquals(1, role.getPrivileges().size());
        assertEquals(Privilege.PIPELINE_MONITOR.toString(), role.getPrivileges().get(0));

        role.addPrivilege(Privilege.PIPELINE_OPERATIONS.toString());
        assertEquals(2, role.getPrivileges().size());
        assertEquals(Privilege.PIPELINE_OPERATIONS.toString(), role.getPrivileges().get(1));

        assertTrue(role.hasPrivilege(Privilege.PIPELINE_MONITOR.toString()));
        assertTrue(role.hasPrivilege(Privilege.PIPELINE_OPERATIONS.toString()));
        assertFalse(role.hasPrivilege(Privilege.PIPELINE_CONFIG.toString()));
        assertFalse(role.hasPrivilege(Privilege.USER_ADMIN.toString()));
    }

    @Test
    public void testAddPrivileges() {
        Role src1 = new Role("src1");
        src1.addPrivilege("a");
        src1.addPrivilege("b");
        Role src2 = new Role("src2");
        src2.addPrivilege("1");
        src2.addPrivilege("2");
        Role src3 = new Role("src3"); // no privileges

        Role dest = new Role("dest");
        assertEquals(0, dest.getPrivileges().size());
        dest.addPrivileges(src1);
        assertEquals(2, dest.getPrivileges().size());
        assertTrue(dest.hasPrivilege("a"));
        assertTrue(dest.hasPrivilege("b"));
        assertFalse(dest.hasPrivilege("1"));
        assertFalse(dest.hasPrivilege("2"));
        dest.addPrivileges(src2);
        assertEquals(4, dest.getPrivileges().size());
        assertTrue(dest.hasPrivilege("a"));
        assertTrue(dest.hasPrivilege("b"));
        assertTrue(dest.hasPrivilege("1"));
        assertTrue(dest.hasPrivilege("2"));
        dest.addPrivileges(src3); // no privileges
        assertEquals(4, dest.getPrivileges().size());
        assertTrue(dest.hasPrivilege("a"));
        assertTrue(dest.hasPrivilege("b"));
        assertTrue(dest.hasPrivilege("1"));
        assertTrue(dest.hasPrivilege("2"));
        dest.addPrivileges(src2); // avoid duplicate privileges
        assertEquals(4, dest.getPrivileges().size());
        assertTrue(dest.hasPrivilege("a"));
        assertTrue(dest.hasPrivilege("b"));
        assertTrue(dest.hasPrivilege("1"));
        assertTrue(dest.hasPrivilege("2"));
    }

    @Test
    public void testName() {
        assertEquals("manager", role.getName());

        String s = "a string";
        role.setName(s);
        assertEquals(s, role.getName());
    }

    @Test
    public void testCreated() {
        assertTrue(role.getCreated() != null);

        Date date = new Date(System.currentTimeMillis());
        role.setCreated(date);
        assertEquals(date, role.getCreated());
    }

    @Test
    public void testCreatedBy() {
        assertNull(role.getCreatedBy());

        User user = new User();
        role.setCreatedBy(user);
        assertEquals(user, role.getCreatedBy());
    }

    @Test
    public void testToString() {
        assertEquals("manager", role.toString());
    }

    @Test
    public void testEqualsObject() {
        assertEquals(role, new Role("manager"));
    }
}
