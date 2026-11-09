package com.memora;

import org.springframework.boot.SpringApplication;

public class TestMemoraBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(MemoraBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
