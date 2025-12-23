package com.genai.java.spring.chat.openai.jailbreak.demo;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class BankingTools {

    @Tool(name = "get-account-balance", description = "Get the current account balance for a given account ID")
    public String getAccountBalance(@ToolParam(description = "The account id to look up") String accountID) {
        if ("12345".equals(accountID)) {
            return "$5,000.00";
        }
        return "Account not found";
    }
}
