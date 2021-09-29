list1 = [1, 2, 3, 3, 4, 5, 4]
list1 + 5
list1 << 6
list1.add(11)

println(list1)

println('list1.isEmpty : => ' + list1.isEmpty())

println('list1.unique : => ' + list1.unique())

println('list1.reverse : => ' + list1.reverse())

println('list1.count : => ' + list1.size())

println('list1.join : => ' + list1.join('-'))

println('list1.sum : => ' + list1.sum())

println('list1.min : => ' + list1.min())

println('list1.max : => ' + list1.max())

println('list1.contains : => ' + list1.contains(1))

// index
println('list1.remove : => ' + list1.remove(1))

println(list1)

list1.each(i -> println(i))

for (i in list1) {
  println(i)
}

println('list1.removeAll : => ' + list1.removeAll())

println(list1)
