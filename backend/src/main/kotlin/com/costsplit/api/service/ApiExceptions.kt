package com.costsplit.api.service

class NotFoundException(message: String) : RuntimeException(message)
class ConflictException(message: String) : RuntimeException(message)
class InvalidRequestException(message: String) : RuntimeException(message)

