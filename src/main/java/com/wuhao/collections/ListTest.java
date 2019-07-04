package com.wuhao.collections;

import java.util.ArrayList;
import java.util.List;

public class ListTest {
    public static void main(String[] args){
        test();
    }

    static void test(){
        List<Integer> integers = new ArrayList<>();
        List<Integer> integersA = new ArrayList<>();

        //添加元素
        integers.add(1);
        integers.add(2);
        integers.add(3);
        integers.add(4);

        integersA.add(1);
        integersA.add(2);
        integersA.add(33);
        integersA.add(44);
        System.out.println("列表大小：" + integers.size());
        System.out.println("是否为空：" + integers.isEmpty());
        System.out.println("是否包含某元素：" + integers.contains(2));
        System.out.println("是否包含全部元素：" + integers.containsAll(integersA));

        //转换为数组
        Integer[] integerArray = integers.toArray(new Integer[0]);
        System.out.println("遍历数组：");
        for (int i = 0; i < integerArray.length; i++){
            System.out.println(integerArray[i]);
        }
        System.out.println("当前列表integers：" + integers);

        //批量添加
        System.out.println("批量添加元素");
        integers.addAll(integersA);
        System.out.println("当前列表integers：" + integers);

        //移除元素
        System.out.println("移除元素");
        integers.remove(1);
        System.out.println("当前列表integers：" + integers);

        //批量移除
        System.out.println("批量移除元素");
        integers.removeAll(integersA);
        System.out.println("当前列表integers：" + integers);

        //开始替换
        System.out.println("批量替换元素");
        integers.replaceAll(it -> it + 1);
        System.out.println("当前列表integers：" + integers);

        //从列表中移除所有不在集合integersA中的元素
        integersA.add(2);
        integersA.add(4);
        System.out.println("保留元素");
        integers.retainAll(integersA);
        System.out.println("当前列表integers：" + integers);

        //插入
        System.out.println("开始插入");
        System.out.println("当前列表integersA：" + integersA);
        integersA.add(2,155);
        integersA.add(1,125);
        System.out.println("当前列表integersA：" + integersA);

        //排序
        System.out.println("开始排序——使用内部比较器");
        integersA.sort(null);
        System.out.println("当前列表integersA：" + integersA);

        System.out.println("开始排序——使用外部比较器");
        integersA.sort((itA, itB) -> itB - itA);
        System.out.println("当前列表integersA：" + integersA);

        //序号操作
        Integer a = integersA.get(2);
        System.out.println("integersA第三个元素是：" + a);
        System.out.println("开始替换");
        integersA.set(3, 66);
        System.out.println("当前列表integersA：" + integersA);
        System.out.println("开始移除");
        integersA.remove(3);
        System.out.println("当前列表integersA：" + integersA);

        //搜索操作
        System.out.println("查找元素2(第一次出现)位置：" + integersA.indexOf(2));
        System.out.println("查找元素2(最后一次出现)位置：" + integersA.lastIndexOf(2));

        //子队列操作
        List<Integer> subList = integersA.subList(0, 4);
        System.out.println("子队列：" + subList);
        subList.add(5);
        subList.add(5);
        subList.add(5);
        System.out.println("当前子列表：" + subList);
        System.out.println("当前列表integersA：" + integersA);

        integersA.add(1, 233);
        integersA.add(1, 233);
        integersA.add(1, 233);
        System.out.println("当前列表integersA：" + integersA);
        System.out.println("当前子列表：" + subList);
    }
}
