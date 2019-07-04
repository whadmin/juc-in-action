package com.wuhao.collections;

import java.util.*;

public class MapTest {

    public static void main(String[] args){
        Map<Integer, Integer> map = new HashMap<>();
        map.put(1,11);
        map.put(2,22);
        map.put(3,33);

        Set<Integer> keys = map.keySet();
        Collection<Integer> values = map.values();
        Set<Map.Entry<Integer,Integer>> entries = map.entrySet();
        Iterator<Map.Entry<Integer,Integer>> iterator = entries.iterator();
        System.out.println(keys);
        System.out.println(values);
        System.out.println(entries);

        System.out.println("按keyset遍历");
        for (Integer key : keys){
            System.out.println("key:" + key + " value:" + map.get(key));
        }

        System.out.println("按键值对遍历");
        for (Map.Entry<Integer,Integer> entry : entries){
            System.out.println("entry:" + entry);
        }

        System.out.println("按iterator遍历");
        while (iterator.hasNext()){
            Map.Entry<Integer,Integer> entry = iterator.next();
            System.out.println("entry:" + entry);
        }

        map.put(2,444);
        map.put(4,44);
        System.out.println("修改后的视图");
        System.out.println(keys);
        System.out.println(values);
        System.out.println(entries);

        keys.add(5);
        values.add(55);
    }
}
