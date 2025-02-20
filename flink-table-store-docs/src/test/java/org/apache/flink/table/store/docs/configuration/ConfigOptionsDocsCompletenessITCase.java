/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.table.store.docs.configuration;

import org.apache.flink.table.store.annotation.Documentation;
import org.apache.flink.table.store.options.ConfigOption;
import org.apache.flink.table.store.utils.Pair;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.flink.table.store.docs.configuration.ConfigOptionsDocGenerator.DEFAULT_PATH_PREFIX;
import static org.apache.flink.table.store.docs.configuration.ConfigOptionsDocGenerator.LOCATIONS;
import static org.apache.flink.table.store.docs.configuration.ConfigOptionsDocGenerator.extractConfigOptions;
import static org.apache.flink.table.store.docs.configuration.ConfigOptionsDocGenerator.getDescription;
import static org.apache.flink.table.store.docs.configuration.ConfigOptionsDocGenerator.processConfigOptions;
import static org.apache.flink.table.store.docs.configuration.ConfigOptionsDocGenerator.stringifyDefault;
import static org.apache.flink.table.store.docs.configuration.ConfigOptionsDocGenerator.typeToHtml;
import static org.assertj.core.api.Fail.fail;

/**
 * This test verifies that all {@link ConfigOption ConfigOptions} in the configured {@link
 * ConfigOptionsDocGenerator#LOCATIONS locations} are documented and well-defined (i.e. no 2 options
 * exist for the same key with different descriptions/default values), and that the documentation
 * does not refer to non-existent options.
 */
public class ConfigOptionsDocsCompletenessITCase {

    @Test
    public void testCompleteness() throws IOException, ClassNotFoundException {
        compareDocumentedAndExistingOptions(
                parseDocumentedOptions(),
                checkWellDefinedAndDeduplicate(findExistingOptions(ignored -> true)));
    }

    private static Map<String, List<ExistingOption>> checkWellDefinedAndDeduplicate(
            Map<String, List<ExistingOption>> allOptions) {
        return allOptions.entrySet().stream()
                .map(
                        (entry) -> {
                            final List<ExistingOption> existingOptions = entry.getValue();
                            final List<ExistingOption> consolidated;
                            if (existingOptions.stream()
                                    .allMatch(option -> option.isSuffixOption)) {
                                consolidated = existingOptions;
                            } else {
                                consolidated =
                                        Collections.singletonList(
                                                existingOptions.stream()
                                                        .reduce(
                                                                (option1, option2) -> {
                                                                    if (option1.equals(option2)) {
                                                                        // we allow multiple
                                                                        // instances of
                                                                        // ConfigOptions with the
                                                                        // same key
                                                                        // if they are identical
                                                                        return option1;
                                                                    } else {
                                                                        // found a ConfigOption pair
                                                                        // with
                                                                        // the same key that aren't
                                                                        // equal
                                                                        // we fail here outright as
                                                                        // this is
                                                                        // not a
                                                                        // documentation-completeness
                                                                        // problem
                                                                        String errorMessage;
                                                                        if (!option1.defaultValue
                                                                                .equals(
                                                                                        option2.defaultValue)) {
                                                                            errorMessage =
                                                                                    String.format(
                                                                                            "Ambiguous option %s due to distinct default values (%s (in %s) vs %s (in %s)).",
                                                                                            option1.key,
                                                                                            option1.defaultValue,
                                                                                            option1
                                                                                                    .containingClass
                                                                                                    .getSimpleName(),
                                                                                            option2.defaultValue,
                                                                                            option2
                                                                                                    .containingClass
                                                                                                    .getSimpleName());
                                                                        } else {
                                                                            errorMessage =
                                                                                    String.format(
                                                                                            "Ambiguous option %s due to distinct descriptions (%s vs %s).",
                                                                                            option1.key,
                                                                                            option1
                                                                                                    .containingClass
                                                                                                    .getSimpleName(),
                                                                                            option2
                                                                                                    .containingClass
                                                                                                    .getSimpleName());
                                                                        }
                                                                        throw new AssertionError(
                                                                                errorMessage);
                                                                    }
                                                                })
                                                        .get());
                            }
                            return Pair.of(entry.getKey(), consolidated);
                        })
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
    }

    private static void compareDocumentedAndExistingOptions(
            Map<String, List<DocumentedOption>> documentedOptions,
            Map<String, List<ExistingOption>> existingOptions) {
        final Collection<String> problems = new ArrayList<>(0);
        // first check that all existing options are properly documented
        existingOptions.forEach(
                (key, supposedStates) -> {
                    List<DocumentedOption> documentedState = documentedOptions.get(key);
                    for (ExistingOption supposedState : supposedStates) {
                        if (documentedState == null || documentedState.isEmpty()) {
                            // option is not documented at all
                            problems.add(
                                    "Option "
                                            + supposedState.key
                                            + " in "
                                            + supposedState.containingClass
                                            + " is not documented.");
                        } else {
                            final Iterator<DocumentedOption> candidates =
                                    documentedState.iterator();
                            boolean matchFound = false;
                            while (candidates.hasNext()) {
                                DocumentedOption candidate = candidates.next();
                                if (supposedState.defaultValue.equals(candidate.defaultValue)
                                        && supposedState.description.equals(
                                                candidate.description)) {
                                    matchFound = true;
                                    candidates.remove();
                                }
                            }
                            if (documentedState.isEmpty()) {
                                documentedOptions.remove(key);
                            }
                            if (!matchFound) {
                                problems.add(
                                        String.format(
                                                "Documentation of %s in %s is outdated. Expected: default=(%s) description=(%s).",
                                                supposedState.key,
                                                supposedState.containingClass.getSimpleName(),
                                                supposedState.defaultValue,
                                                supposedState.description));
                            }
                        }
                    }
                });

        // documentation contains an option that no longer exists
        documentedOptions.values().stream()
                .flatMap(Collection::stream)
                .forEach(
                        documentedOption ->
                                problems.add(
                                        "Documented option "
                                                + documentedOption.key
                                                + " does not exist."));
        if (!problems.isEmpty()) {
            StringBuilder stringBuilder =
                    new StringBuilder(
                            "Documentation is outdated, please regenerate it according to the"
                                    + " instructions in flink-table-store-docs/README.md.");
            stringBuilder.append(System.lineSeparator());
            stringBuilder.append("\tProblems:");
            for (String problem : problems) {
                stringBuilder.append(System.lineSeparator());
                stringBuilder.append("\t\t");
                stringBuilder.append(problem);
            }
            fail(stringBuilder.toString());
        }
    }

    private static Map<String, List<DocumentedOption>> parseDocumentedOptions() throws IOException {
        final String rootDir = ConfigOptionsDocGeneratorTest.getProjectRootDir();
        Path includeFolder =
                Paths.get(rootDir, "docs", "layouts", "shortcodes", "generated").toAbsolutePath();
        return Files.list(includeFolder)
                .filter(
                        (path) -> {
                            final String filename = path.getFileName().toString();
                            return filename.endsWith("configuration.html");
                        })
                .flatMap(
                        file -> {
                            try {
                                return parseDocumentedOptionsFromFile(file).stream();
                            } catch (IOException ignored) {
                                return Stream.empty();
                            }
                        })
                .collect(Collectors.groupingBy(option -> option.key, Collectors.toList()));
    }

    private static Collection<DocumentedOption> parseDocumentedOptionsFromFile(Path file)
            throws IOException {
        Document document = Jsoup.parse(file.toFile(), StandardCharsets.UTF_8.name());
        document.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
        document.outputSettings().prettyPrint(false);
        return document.getElementsByTag("table").stream()
                .map(element -> element.getElementsByTag("tbody").get(0))
                .flatMap(element -> element.getElementsByTag("tr").stream())
                .map(
                        tableRow -> {
                            // Use split to exclude document key tag.
                            String key = tableRow.child(0).text().split(" ")[0];
                            String defaultValue = tableRow.child(1).text();
                            String typeValue = tableRow.child(2).html();
                            String description =
                                    tableRow.child(3).childNodes().stream()
                                            .map(Object::toString)
                                            .collect(Collectors.joining());
                            return new DocumentedOption(key, defaultValue, typeValue, description);
                        })
                .collect(Collectors.toList());
    }

    private static Map<String, List<ExistingOption>> findExistingOptions(
            Predicate<ConfigOptionsDocGenerator.OptionWithMetaInfo> predicate)
            throws IOException, ClassNotFoundException {
        final String rootDir = ConfigOptionsDocGeneratorTest.getProjectRootDir();
        final Collection<ExistingOption> existingOptions = new ArrayList<>();
        for (OptionsClassLocation location : LOCATIONS) {
            processConfigOptions(
                    rootDir,
                    location.getModule(),
                    location.getPackage(),
                    DEFAULT_PATH_PREFIX,
                    optionsClass ->
                            extractConfigOptions(optionsClass).stream()
                                    .filter(predicate)
                                    .map(
                                            optionWithMetaInfo ->
                                                    toExistingOption(
                                                            optionWithMetaInfo, optionsClass))
                                    .forEach(existingOptions::add));
        }
        return existingOptions.stream()
                .collect(Collectors.groupingBy(option -> option.key, Collectors.toList()));
    }

    private static ExistingOption toExistingOption(
            ConfigOptionsDocGenerator.OptionWithMetaInfo optionWithMetaInfo,
            Class<?> optionsClass) {
        String key = optionWithMetaInfo.option.key();
        String defaultValue = stringifyDefault(optionWithMetaInfo);
        String typeValue = typeToHtml(optionWithMetaInfo);
        String description = getDescription(optionWithMetaInfo);
        boolean isSuffixOption = isSuffixOption(optionWithMetaInfo.field);
        return new ExistingOption(
                key, defaultValue, typeValue, description, optionsClass, isSuffixOption);
    }

    private static boolean isSuffixOption(Field field) {
        final Class<?> containingOptionsClass = field.getDeclaringClass();

        return field.getAnnotation(Documentation.SuffixOption.class) != null
                || containingOptionsClass.getAnnotation(Documentation.SuffixOption.class) != null;
    }

    private static final class ExistingOption extends Option {

        private final Class<?> containingClass;
        private final boolean isSuffixOption;

        private ExistingOption(
                String key,
                String defaultValue,
                String typeValue,
                String description,
                Class<?> containingClass,
                boolean isSuffixOption) {
            super(key, defaultValue, typeValue, description);
            this.containingClass = containingClass;
            this.isSuffixOption = isSuffixOption;
        }
    }

    private static final class DocumentedOption extends Option {

        private DocumentedOption(
                String key, String defaultValue, String typeValue, String description) {
            super(key, defaultValue, typeValue, description);
        }
    }

    private abstract static class Option {
        protected final String key;
        protected final String defaultValue;
        protected final String typeValue;
        protected final String description;

        private Option(String key, String defaultValue, String typeValue, String description) {
            this.key = key;
            this.defaultValue = defaultValue;
            this.typeValue = typeValue;
            this.description = description;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Option option = (Option) o;
            return Objects.equals(key, option.key)
                    && Objects.equals(defaultValue, option.defaultValue)
                    && Objects.equals(typeValue, option.typeValue)
                    && Objects.equals(description, option.description);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, defaultValue, typeValue, description);
        }

        @Override
        public String toString() {
            return "Option{"
                    + "key='"
                    + key
                    + '\''
                    + ", defaultValue='"
                    + defaultValue
                    + '\''
                    + ", typeValue='"
                    + typeValue
                    + '\''
                    + ", description='"
                    + description
                    + '\''
                    + '}';
        }
    }
}
