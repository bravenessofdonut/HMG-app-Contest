package com.braveberry.voice.remote.mapper

/**
 * Firebase AI 예외의 (message, cause) 생성자는 Kotlin internal이라 테스트 코드에서 직접 호출할 수 없음.
 * JVM에서는 public이므로 리플렉션으로 실제 인스턴스를 만들어 타입·원인 체인 분류를 그대로 검증함.
 */
internal inline fun <reified T : Throwable> firebaseError(cause: Throwable? = null): T =
    T::class.java
        .getConstructor(String::class.java, Throwable::class.java)
        .newInstance("test", cause)
