package com.example.demo;

import com.example.demo.cache.QueryResultCache;
import com.example.demo.entity.QueryJob;
import com.example.demo.entity.enums.QueryJobStatus;
import com.example.demo.repository.QueryJobRepository;
import com.example.demo.service.JobExecutionService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}
	@Bean
	CommandLineRunner asyncTest(JobExecutionService jobExec,
								QueryJobRepository jobRepo, QueryResultCache queryResultCache) {
		return args -> {
			// create a fake job
			System.out.println("Main thread: " + Thread.currentThread().getName());
			QueryJob job = new QueryJob(4L, QueryJobStatus.QUEUED);
			job = jobRepo.save(job);

			System.out.println("Submitting async job " + job.getId());
			jobExec.executeJobAsync(job.getId());

			// keep main thread alive a bit so async work finishes
			Thread.sleep(3000);

			QueryJob refreshed = jobRepo.findById(job.getId()).orElseThrow();
			if(queryResultCache.contains(4L) && !queryResultCache.get(4L).isEmpty()){
				System.out.println("cache is caching xd");
				System.out.println(queryResultCache.get(4L));
			}
			System.out.println("Final status = " + refreshed.getStatus());
		};
	}

}
