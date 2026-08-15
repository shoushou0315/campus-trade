package com.campus.trade.mapper;

import com.campus.trade.entity.UserDO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserMapperTest {

    @Autowired
    private UserMapper userMapper;

    @Test
    void testSelectById() {
        UserDO user = userMapper.selectById(1L);
        assertNotNull(user);
        assertEquals("zhangsan", user.getUsername());
        assertEquals("三哥", user.getNickname());
        assertEquals("武汉大学", user.getSchool());
        System.out.println("查询结果: " + user);
    }

    @Test
    void testSelectByUsername() {
        UserDO user = userMapper.selectByUsername("lisi");
        assertNotNull(user);
        assertEquals("小李同学", user.getNickname());
        System.out.println("按用户名查询: " + user);
    }

    @Test
    void testSelectByUsernameNotFound() {
        UserDO user = userMapper.selectByUsername("notexist");
        assertNull(user);
    }

    @Test
    void testInsert() {
        UserDO user = new UserDO();
        user.setUsername("testuser" + System.currentTimeMillis());
        user.setPassword("testpass");
        user.setNickname("测试用户");
        user.setPhone("13800000003");
        user.setSchool("武汉大学");

        int rows = userMapper.insert(user);
        assertEquals(1, rows);
        assertNotNull(user.getId());

        UserDO saved = userMapper.selectById(user.getId());
        assertNotNull(saved);
        assertEquals(user.getUsername(), saved.getUsername());

        System.out.println("插入成功, id=" + user.getId());
    }
}
