package com.github.m9w

import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method


class ReflectTool<T> private constructor(private val targetClass: Class<T>) {
    private var targetObject: T? = null
    fun usage(o: T): ReflectTool<T> {
        targetObject = o
        return this
    }

    fun <U> method(methodName: String, vararg obj: Class<*>): _Method<U> {
        return _Method(methodName, *obj)
    }

    operator fun <U> invoke(name: String, vararg obj: Any): U {
        return _Method<U>(name, *obj.map { o: Any -> o.javaClass }.toTypedArray()).invoke(*obj)
    }

    fun <U> getField(field: String): U {
        return _Field<U>(field).get()
    }

    fun <U> setField(field: String, value: U) {
        _Field<U>(field).set(value)
    }

    fun <U> field(field: String): _Field<U> {
        return _Field(field)
    }

    inner class _Method<U> internal constructor(name: String, vararg obj: Class<*>) {
        private val method: Method = targetClass.getDeclaredMethod(name, *obj)

        init {
            method.isAccessible = true
        }

        operator fun invoke(vararg obj: Any?) = method.invoke(targetObject, *obj) as U
    }

    inner class _Field<U> internal constructor(name: String) {
        private val field: Field = targetClass.getDeclaredField(name)

        init {
            field.isAccessible = true
        }

        fun set(obj: U) {
            field[targetObject] = obj
        }

        fun get() = field[targetObject] as U
    }

    companion object {
        @JvmStatic
        fun <T: Any> of(o: T): ReflectTool<T> {
            return ReflectTool(o::class.java as Class<T>).usage(o)
        }
        @JvmStatic
        fun <T> of(o: Class<T>): ReflectTool<T> {
            return ReflectTool(o)
        }
    }
}

