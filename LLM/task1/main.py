s = "Pyth1abch2hon"
first = s.find("h")
last = s.rfind("h")
print(s[: first + 1] + s[first + 1 : last][::-1] + s[last:])

massive = sorted((11, -17, 8, -120, -35, 21, 87, 115, -240), key=lambda x: abs(x), reverse=True)
print("min: ", min(abs(x) for x in massive), "max: ", max(abs(x) for x in massive), "massive: ", massive)

size = int(input("Enter the size of the list: "))
if size > 2:
    massive = []
    print("Enter the elements of the list: ")
    for i in range(size):
        massive.append(int(input()))
    print("Original list: ", massive)
    massive[0], massive[-2] = massive[-2], massive[0]
    print("Updated list: ", massive)
else:
    print("Size of the list is less than 2")

def sum_1(n):
    return sum(1/i**2 for i in range(1, n+1))
def sum_2(n, m):
    return sum(i**n for i in range(1, m+1))
def select_operation(operation):
    if operation == 1:
        return sum_1
    else:
        return sum_2
print(select_operation(1)(10))
print(select_operation(2)(10, 10))

string = input("Enter the string: ")
if string == string[::-1]:
    print("The string is a palindrome")
else:
    print("The string is not a palindrome")

letters = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z']
numbers = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26]
dictionary = dict(zip(letters, numbers))
print(dictionary)
print(sum(dictionary.values()))

m = [3.46871, 5.31916, 4.013449, 6.57686, 15.012678]
new_m = list(map(round, m, range(1, len(m) + 1)))
print(new_m)

n = int(input("Enter the number: "))
for i in range(1, n + 1):
    print('$' + ' ' * (i-1) + '$')