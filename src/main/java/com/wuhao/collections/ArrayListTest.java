package com.wuhao.collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class ArrayListTest {

    private List example;

    @Before
    public void setUp() {
        example = new ArrayList();
    }

    @After
    public void tearDown() {
        example = null;
    }

    @Test
    public void testAddFirst() {
        IntStream.range(0, 50).forEach(p -> {
            example.add(p);
        });
    }
}
