package org.example.Java.sorting;

public class Student implements Comparable<Student>{
    private final String name;
    private final int age;

    public Student(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public Student(String name) {
        this.name = name;
        this.age = 18;
    }

    public Student(int age) {
        this.name = "Bool";
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    @Override
    public int compareTo(Student o) {
        if(this.name.equals(o.name)) return Integer.compare(this.age, o.age);
        Integer.compare(this.age, o.age);
        return this.name.compareTo(o.name);
    }

    @Override
    public String toString() {
        return name + " " + age;
    }
}
