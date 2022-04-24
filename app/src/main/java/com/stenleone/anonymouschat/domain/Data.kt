package com.stenleone.anonymouschat.domain

sealed interface Data<T> {
    class Success<T>(val data: T): Data<T>
    class Error<T>(val error: String): Data<T>
}