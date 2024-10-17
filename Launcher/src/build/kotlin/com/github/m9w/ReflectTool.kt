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
        private val method: Method

        init {
            try {
                method = targetClass.getDeclaredMethod(name, *obj)
                method.isAccessible = true
            } catch (e: NoSuchMethodException) {
                throw RuntimeException(e)
            }
        }

        operator fun invoke(vararg obj: Any?): U {
            return try {
                method.invoke(targetObject, *obj) as U
            } catch (e: InvocationTargetException) {
                throw RuntimeException(e)
            } catch (e: IllegalAccessException) {
                throw RuntimeException(e)
            }
        }
    }

    inner class _Field<U> internal constructor(name: String) {
        private val field: Field

        init {
            try {
                field = targetClass.getDeclaredField(name)
                field.isAccessible = true
            } catch (e: NoSuchFieldException) {
                throw RuntimeException(e)
            }
        }

        fun set(obj: U) {
            try {
                field[targetObject] = obj
            } catch (e: IllegalAccessException) {
                throw RuntimeException(e)
            }
        }

        fun get(): U {
            return try {
                field[targetObject] as U
            } catch (e: IllegalAccessException) {
                throw RuntimeException(e)
            }
        }
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

