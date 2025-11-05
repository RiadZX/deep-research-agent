package com.embabel.template.agent;

import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.Condition;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.agent.core.CoreToolGroups;
import com.embabel.agent.domain.io.UserInput;
import com.embabel.agent.domain.library.HasContent;
import com.embabel.agent.prompt.persona.RoleGoalBackstory;
import com.embabel.common.core.types.Timestamped;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

abstract class Personas {
    static final RoleGoalBackstory RESEARCHER = RoleGoalBackstory
            .withRole("Deep Web Researcher")
            .andGoal("Perform thorough research on any topic")
            .andBackstory("Experienced in using advanced web tools and techniques to gather information from the web; has researched thousands of topics, published numerous reports.");
}

@Agent(description = "Perform deep web research on a topic and compile findings into a report.")
public class Researcher {
    Researcher(){

    }

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
            pre = {},
            canRerun = true
    )
    ResearchTask craftResearchTask(UserInput userInput, OperationContext context) {
        return context.ai()
                .withAutoLlm()
                .withToolGroup(CoreToolGroups.WEB)
                .withToolGroup(CoreToolGroups.BROWSER_AUTOMATION)
                .withPromptContributor(Personas.RESEARCHER)
                .createObject(String.format("""
                        Create detailed research tasks based on the user input.Come up with a clear research topic and a list of specific queries to be answered.
                        
                        Example:
                        Userinput: "I want to learn about the history of the Eiffel Tower."
                        
                        Research Topic: "Eiffel Tower's History and Evolution"
                        Research Queries:
                        1) Investigate the foundational purpose and date of the Eiffel Tower's construction, focusing on its role as the entrance arch for the 1889 Exposition Universelle in Paris.
                        (2) Research the design process, the primary architects and engineers involved (notably Gustave Eiffel), and the technological challenges faced during the construction phase.
                        (3) Explore the immediate public and artistic reception of the tower, including the initial controversies, petitions, and the contractual understanding that it was meant to be temporary.
                        (4) Analyze the tower's earliest functions in the late 19th and early 20th centuries, specifically its scientific uses for meteorology, aerodynamics, and early experiments in telegraphy and radio broadcasting.
                        (5) Document the critical decisions and events that ultimately prevented the tower's planned demolition, focusing on its strategic value for military communications.
                        (6) Track the evolution of the tower's role throughout the 20th century, detailing major renovations, technological upgrades, and the installation of subsequent television and radio antennae.
                        (7) Identify and summarize notable historical events, celebrations, and periods of international conflict (such as WWI and WWII) directly connected to the history and operation of the Eiffel Tower.
                        
                        Now, create a research task for the following user input:
                        %s
                        
                        
                        -----
                        
                        Make sure to ask the user for confirmation. If the user confirmed it, set confirmed_by_user to true, otherwise false.
                        """, userInput.getContent()).trim(), ResearchTask.class);
    }










}

