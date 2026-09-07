package org.example.Java.sorting;

import java.util.List;

public class StudentSort implements Sorting<Student>{
    public void sort(List<Student> students) {
//        students.sort(Comparator.comparing(Student::getName).thenComparing(Student::getAge));
        students.sort(Student::compareTo);
    }
}
