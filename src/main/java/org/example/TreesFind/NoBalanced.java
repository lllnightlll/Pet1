package org.example.TreesFind;

import java.util.Scanner;

public class NoBalanced {
    private Tree tree;
    private final Scanner in = new Scanner(System.in);

    private class Tree {
        private int x;
        private Tree L, R;

        private Tree(int x) {
            this.x = x;
        }

        private void insert(int x) {
            if (this.x == x) {
                return;
            } else if (this.x > x) {
                if (L == null) {
                    L = new Tree(x);
                }
                L.insert(x);
            } else {
                if (R == null) {
                    R = new Tree(x);
                }
                R.insert(x);
            }
        }

        private Tree delete(int x) {
            if (this.x < x) {
                if (L != null) {
                    L = L.delete(x);
                }
                return this;
            } else if (this.x > x) {
                if (R != null) {
                    R = R.delete(x);
                }
                return this;
            } else {
                if (L == null && R == null) return null;

                if (L == null) return R;
                if (R == null) return L;

                Tree temp = L;
                while (temp.R != null) temp = temp.R;
                this.x = temp.x;
                L = L.delete(temp.x);
                return this;
            }
        }

        private boolean has(int x) {
            if (this.x == x) {
                return true;
            } else if (this.x > x) {
                if (L != null) {
                    return L.has(x);
                }
            } else if (this.x < x) {
                if (R != null) {
                    return R.has(x);
                }
            }
            return false;
        }

        private Integer prev(int x) {
            Integer mojetBbltb = null;
            if (this.x < x) {
                mojetBbltb = this.x;
                if (R != null) {
                    Integer temp = R.prev(x);
                    if (temp != null) {
                        mojetBbltb = temp;
                    }
                }
            } else if (L != null) {
                mojetBbltb = L.prev(x);
            }
            return mojetBbltb;
        }

        private Integer next(int x) {
            Integer mojetBbltb = null;
            if (this.x > x) {
                mojetBbltb = this.x;
                if (L != null) {
                    Integer temp = L.next(x);
                    if (temp != null) {
                        mojetBbltb = temp;
                    }
                }
            } else if (R != null) {
                mojetBbltb = R.next(x);
            }
            return mojetBbltb;
        }
    }


    public NoBalanced() {
        run();
    }

    private void run() {
        for (int i = 0; i < 100; i++) {
            String text = in.next();
            int number = toInt(in.next());
            switch (text) {
                case "insert":
                    insert(number);
                    break;
                case "delete":
                    delete(number);
                    break;
                case "has":
                    has(number);
                    break;
                case "next":
                    next(number);
                    break;
                case "prev":
                    prev(number);
                    break;
                default:
                    System.out.println("АШИБКА");
            }
        }
    }

    private int toInt(String x) {
        try {
            return Integer.parseInt(x);
        } catch (Exception ex) {
            int number = 0;
            for (char i : x.toCharArray()) {
                number += (int)i;
            }
            return number;
        }
    }

    private void insert(int x) {
        if (tree == null) {
            tree = new Tree(x);
        } else {
            tree.insert(x);
        }
    }

    private void delete(int x) {
        if (tree == null) return;
        tree = tree.delete(x);
    }

    private void has(int x) {
        if (tree == null) {
            System.out.println("f");
            return;
        }
        if (tree.has(x)) {
            System.out.println("t");
        } else {
            System.out.println("f");
        }
    }

    private void next(int x) {
        System.out.println(tree.next(x));
    }

    private void prev(int x) {
        System.out.println(tree.prev(x));
    }
}
