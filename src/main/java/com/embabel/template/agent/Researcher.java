package com.embabel.template.agent;

import com.embabel.agent.api.annotation.*;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.agent.api.models.OpenAiModels;
import com.embabel.agent.core.CoreToolGroups;
import com.embabel.agent.domain.io.UserInput;
import com.embabel.agent.domain.library.HasContent;
import com.embabel.agent.prompt.persona.RoleGoalBackstory;
import com.embabel.common.ai.model.LlmOptions;
import com.embabel.common.core.types.Timestamped;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

abstract class PersonasResearch {
    static final RoleGoalBackstory RESEARCHER = RoleGoalBackstory
            .withRole("Deep Web Researcher")
            .andGoal("Perform thorough research on any topic")
            .andBackstory("Experienced in using advanced web tools and techniques to gather information from the web; has researched thousands of topics, published numerous reports.");
}

@Agent(description = "Perform deep web research on a topic and compile findings into a report.")
@SuppressWarnings("unused")
public class Researcher {
    Researcher(){

    }

    public static final String TASK_CONFIRMED = "task_confirmed";
    public static final String TASK_NOT_CONFIRMED = "task_not_confirmed";


    public record ResearchTask(String topic, List<String> queries, boolean confirmed_by_user) {}

    public record ResearchReport(String topic, String findings) implements HasContent, Timestamped {

        @NotNull
        @Override
        public String getContent() {
            return  String.format("Topic : %s\nFindings : %s\n %s",topic,findings, getTimestamp().atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy"))).trim();
        }

        @NotNull
        @Override
        public Instant getTimestamp() {
            return Instant.now();
        }
    }

    @Action(
            canRerun = true
    )
    ResearchTask craftResearchTask(UserInput userInput, OperationContext context) {
        return context.ai()
                .withAutoLlm()
                .withToolGroup(CoreToolGroups.WEB)
                .withToolGroup(CoreToolGroups.BROWSER_AUTOMATION)
                .withPromptContributor(PersonasResearch.RESEARCHER)
                .createObject(String.format("""
                        Create detailed research tasks based on the user input.Come up with a clear research topic and a list of specific queries to be answered.
                        
                        Example:
                        Userinput: "I want to learn about the history of the Eiffel Tower."
                        
                        Research Topic: "Eiffel Tower's History and Evolution"
                        Research Queries:
                        (1) Investigate the foundational purpose and date of the Eiffel Tower's construction, focusing on its role as the entrance arch for the 1889 Exposition Universelle in Paris.
                        (2) Research the design process, the primary architects and engineers involved (notably Gustave Eiffel), and the technological challenges faced during the construction phase.
                        (3) Explore the immediate public and artistic reception of the tower, including the initial controversies, petitions, and the contractual understanding that it was meant to be temporary.
                        (4) Analyze the tower's earliest functions in the late 19th and early 20th centuries, specifically its scientific uses for meteorology, aerodynamics, and early experiments in telegraphy and radio broadcasting.
                        (5) Document the critical decisions and events that ultimately prevented the tower's planned demolition, focusing on its strategic value for military communications.
                        (6) Track the evolution of the tower's role throughout the 20th century, detailing major renovations, technological upgrades, and the installation of subsequent television and radio antennae.
                        (7) Identify and summarize notable historical events, celebrations, and periods of international conflict (such as WWI and WWII) directly connected to the history and operation of the Eiffel Tower.
                        
                        Now, create a research task for the following user input:
                        %s
                        
                        
                        -----
                        
                        set confirmed_by_user to `false`.
                        DO NOT REPLY WITH ANYTHING OTHER THAN THE RESEARCH TASK OBJECT.
                        """, userInput.getContent()).trim(), ResearchTask.class);
    }

    @Action(
            canRerun = true,
            description = "Confirm the research task with the user.",
            post = {TASK_CONFIRMED}
    )
    ResearchTask confirmTask(ResearchTask researchTask, OperationContext context) {
        // Confirm with the user
        // print the research task details and ask for confirmation
        System.out.println("Research Topic: " + researchTask.topic);
        System.out.println("Research Queries:");
        for (String query : researchTask.queries) {
            System.out.println("- " + query);
        }
        System.out.println("Do you confirm this research task? (yes/no)");
        // use scanner to get user input
        Scanner scanner = new Scanner(System.in);
        String confirmation = scanner.nextLine();
        boolean confirmed = confirmation.equalsIgnoreCase("yes");

        if (confirmed) {
            return new ResearchTask(researchTask.topic, researchTask.queries, true);
        }

        System.out.println("What would you like to change?");
        String changes = scanner.nextLine();

        return context.ai()
                .withAutoLlm()
                .withToolGroup(CoreToolGroups.WEB)
                .withToolGroup(CoreToolGroups.BROWSER_AUTOMATION)
                .withPromptContributor(PersonasResearch.RESEARCHER)
                .createObject(String.format("""
                        The user has requested changes to the following research task.
                        
                        Original Task:
                        Topic: %s
                        Queries:
                        %s
                        
                        User's requested changes:
                        %s
                        
                        Please generate a new research task that incorporates these changes.
                        Set confirmed_by_user to `false`.
                        DO NOT REPLY WITH ANYTHING OTHER THAN THE RESEARCH TASK OBJECT.
                        """, researchTask.topic, String.join("\n", researchTask.queries), changes).trim(), ResearchTask.class);
    }

    @AchievesGoal(
            description = "A comprehensive research report has been compiled on the specified topic.",
            export = @Export(remote = true, name = "researchReport")
    )
    @Action(
            pre = {TASK_CONFIRMED}
    )
    ResearchReport compileResearchReport(ResearchTask researchTask, OperationContext context) {
        var findings = context.ai()
                .withLlm(LlmOptions
                        .withModel(OpenAiModels.GPT_41)
                        .withTemperature(0.3))
                .withToolGroup(CoreToolGroups.WEB)
                .withToolGroup(CoreToolGroups.BROWSER_AUTOMATION)
                .withPromptContributor(PersonasResearch.RESEARCHER)
                .generateText(String.format("""
                        Using the following research queries, gather information from reliable web sources and compile a comprehensive research report on the topic: %s.
                        
                        Research Queries:
                        %s
                        
                        Provide detailed findings for each query and synthesize them into a coherent report.
                        Add the sources as links at the end of the report.
                        """, researchTask.topic, String.join("\n", researchTask.queries)).trim());
        return new ResearchReport(researchTask.topic, findings);
    }

    @Condition(name = TASK_CONFIRMED)
    public boolean isTaskConfirmed(ResearchTask researchTask) {
        return researchTask.confirmed_by_user;
    }

    @Condition(name = TASK_NOT_CONFIRMED)
    public boolean isTaskNotConfirmed(ResearchTask researchTask) {
        return !researchTask.confirmed_by_user;
    }

}

