package com.wuhao.collections.concurrent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CopyOnWriteArrayList;

public class CopyOnWriteArrayListExampleTest {

    private CopyOnWriteArrayList example;

    @Before
    public void setUp() {
        example = new CopyOnWriteArrayList();
    }

    @After
    public void tearDown() {
        example = null;
    }

    @Test
    public void testAddFirst(){

    }
}
