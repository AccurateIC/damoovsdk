package com.example.accuratedamoov

open class Demo(val name: String = "Omkar") {
    init {
        println("Demo initialized: $name")
    }

    open fun printMessage() {
        println("Hello from subclass")
    }
}


final class SubClass() : Demo() {
    init {
        println("subClass initialized:  "+super.name)
    }
    override fun printMessage() {
        println("Hello from subclass")
    }
}



fun main() {
    println("hi Omkar")
    val demo = SubClass()
    fun SubClass.printName(name: String){

        println("updated name: "+name)
    }


    demo.printName("Omkar Prabhale")

}