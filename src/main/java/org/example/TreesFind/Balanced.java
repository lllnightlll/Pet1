package org.example.TreesFind;

import java.util.Scanner;

public class Balanced {
    private Tree tree;
    private final Scanner in = new Scanner(System.in);

    private class Tree {
        private int x;
        private Tree L, R;
        private int h = 1;

        private Tree(int x) {
            this.x = x;
        }

        private int height(Tree n) {
            if (n == null) {
                return 0;
            } else {
                return n.h;
            }
        }

        private int balance(Tree n) {
            if (n == null) {
                return 0;
            } else {
                return height(n.L)-height(n.R);
            }
        }

        private Tree right() {
            Tree y = this.L;
            Tree T2 = y.R;

            y.R = this;
            this.L = T2;

            this.h = 1 + Math.max(height(this.L), height(this.R));
            y.h   = 1 + Math.max(height(y.L),   height(y.R));
            return y;
        }

        private Tree left() {
            Tree y = this.R;
            Tree T2 = y.L;

            y.L = this;
            this.R = T2;

            this.h = 1 + Math.max(height(this.L), height(this.R));
            y.h   = 1 + Math.max(height(y.L),   height(y.R));
            return y;
        }

        private Tree insert(int x) {
            if (this.x == x) {
                return this;
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

            h = 1 + Math.max(height(L), height(R));
            int bf = balance(this);

            if (bf > 1 && x < L.x) {
                return right();
            } else if (bf < -1 && x > R.x) {
                return left();
            } else if (bf > 1 && x > L.x) {
                L = L.left();
                return right();
            } else if (bf < -1 && x < R.x) {
                R = R.right();
                return left();
            }
            return this;
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


    public Balanced() {
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
                number += (int) i;
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
