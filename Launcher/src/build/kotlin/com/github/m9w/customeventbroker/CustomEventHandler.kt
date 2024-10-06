package com.github.m9w.customeventbroker

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class CustomEventHandler(val value: String = "", val regEx: String = "")
