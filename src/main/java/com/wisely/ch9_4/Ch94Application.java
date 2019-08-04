package com.wisely.ch9_4;

import java.io.File;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.dsl.file.Files;
import org.springframework.integration.feed.inbound.FeedEntryMessageSource;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.mail.dsl.Mail;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.messaging.handler.annotation.Payload;

import com.rometools.rome.feed.synd.SyndEntry;

@SpringBootApplication
public class Ch94Application {

	public static void main(String[] args) {
		SpringApplication.run(Ch94Application.class, args);
	}
	

	@Value("https://spring.io/blog.atom")
	Resource resource;
	
	@Bean(name = PollerMetadata.DEFAULT_POLLER)
	public PollerMetadata poller() {
		return Pollers.fixedRate(500).get();
	}
	
	@Bean
	public FeedEntryMessageSource feedMessageSource() throws IOException {
		FeedEntryMessageSource messageSource = new FeedEntryMessageSource(resource.getURL(),"news");
		return messageSource;
	}
	
	@Bean
	public IntegrationFlow myFlow() throws IOException {
		return IntegrationFlows.from(feedMessageSource())
				.<SyndEntry,String>route(payload -> 
				payload.getCategories().get(0).getName(), 
						mapping -> mapping.channelMapping("releases", "releasesChannel")
						.channelMapping("engineering", "engineeringChannel")
						.channelMapping("news", "newsChannel"))
				.get();
	}
	
	@Bean
	public IntegrationFlow releasesFlow() {
		return IntegrationFlows.from(MessageChannels.queue("releasesChannel", 10))
				.<SyndEntry,String>transform(
						payload -> "《" +payload.getTitle()+"》"+
								payload.getLink() /* + getProperty("line.separator") */)
				.handle(Files.outboundAdapter(new File("D:/Program Files/DevelopmentTools/test/springblog"))
						.fileExistsMode(FileExistsMode.APPEND)
						.charset("UTF-8")
						.fileNameGenerator(message -> "releases.txt")
						.get())
				.get();
	}
	
	@Bean
	public IntegrationFlow engineeringFlow() {
		return IntegrationFlows.from(MessageChannels.queue("engineeringChannel", 10))
				.<SyndEntry,String>transform(e -> "《"+e.getTitle()+"》"+e.getLink()
				/* +getProperty("line.separator") */)
				.handle(Files.outboundAdapter(new File("D:/Program Files/DevelopmentTools/test/springblog"))
						.fileExistsMode(FileExistsMode.APPEND)
						.charset("UTF-8")
						.fileNameGenerator(message -> "engineering.txt")
						.get())
				.get();
	}
	
	@Bean
	public IntegrationFlow newsFlow() {
		return IntegrationFlows.from(MessageChannels.queue("newsChannel", 10))
				.<SyndEntry,String>transform(payload->"《"+payload.getTitle()+"》"+
						payload.getLink() /* +getProperty("line.separator") */ )
				.enrichHeaders(Mail.headers()
						.subject("来自Spring的新闻")
						.to("winterwei@aliyun.com")
						.from("837144154@qq.com"))
				.handle(Mail.outboundAdapter("smtp.qq.com")
						.port(25)
						.protocol("smtp")
						.credentials("837144154@qq.com", "weiwentao1318")
						.javaMailProperties(p -> p.put("mail.debug", false)),
						e -> e.id("smtpOut"))
				.get();
	}
	
	
}




