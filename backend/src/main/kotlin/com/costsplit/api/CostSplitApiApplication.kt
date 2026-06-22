package com.costsplit.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CostSplitApiApplication

fun main(args: Array<String>) {
	runApplication<CostSplitApiApplication>(*args)
}
