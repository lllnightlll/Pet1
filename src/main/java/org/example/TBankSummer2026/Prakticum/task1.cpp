/*#include <bits/stdc++.h>
using namespace std;

int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    int t;
    cin >> t;
    while (t--) {
        string s;
        cin >> s;
        int a = 0;
        for (char c : s) {
            if (c == 'A') {
                a++;
            }
        }
        int swaps = 0;
        for (int i = 0; i < a; i++) {
            if (s[i] == 'B') {
                swaps++;
            }
        }
        cout << swaps << '\n';
    }
    return 0;
}*/