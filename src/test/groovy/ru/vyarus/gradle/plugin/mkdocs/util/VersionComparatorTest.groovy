package ru.vyarus.gradle.plugin.mkdocs.util

import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 07.12.2021
 */
class VersionComparatorTest extends Specification {

    def "Check strings sorting"() {

        when: "sorting alpha-numeric string, zero last"
        List<String> ORDERED = ['1ab', '5ab', '10ab', 'a1b', 'a5b', 'a10b', 'ab1', 'ab5', 'ab10']
        def list = new ArrayList(ORDERED)
        Collections.shuffle(list)
        Collections.sort(list, VersionsComparator.comparingVersions(false))

        then: "order correct"
        list == ORDERED

        when: "sorting alpha-numeric string, zero first"
        list = new ArrayList(ORDERED)
        Collections.shuffle(list)
        Collections.sort(list, VersionsComparator.comparingVersions(true))

        then: "order correct"
        list == ORDERED
    }

    def "Check reversed sorting"() {

        when: "sorting alpha-numeric string, zero last"
        List<String> ORDERED = ['1ab', '5ab', '10ab', 'a1b', 'a5b', 'a10b', 'ab1', 'ab5', 'ab10'].reverse()
        def list = new ArrayList(ORDERED)
        Collections.shuffle(list)
        Collections.sort(list, VersionsComparator.comparingVersions(false).reversed())

        then: "order correct"
        list == ORDERED

        when: "sorting alpha-numeric string, zero first"
        list = new ArrayList(ORDERED)
        Collections.shuffle(list)
        Collections.sort(list, VersionsComparator.comparingVersions(true).reversed())

        then: "order correct"
        list == ORDERED
    }

    def "Check double reversed strings sorting"() {

        when: "sorting alpha-numeric string, zero last"
        List<String> ORDERED = ['1ab', '5ab', '10ab', 'a1b', 'a5b', 'a10b', 'ab1', 'ab5', 'ab10']
        def list = new ArrayList(ORDERED)
        Collections.shuffle(list)
        Collections.sort(list, VersionsComparator.comparingVersions(false).reversed().reversed())

        then: "order correct"
        list == ORDERED

        when: "sorting alpha-numeric string, zero first"
        list = new ArrayList(ORDERED)
        Collections.shuffle(list)
        Collections.sort(list, VersionsComparator.comparingVersions(true).reversed().reversed())

        then: "order correct"
        list == ORDERED
    }

    def "Check zeros sorting"() {

        when: "sorting alpha-numeric string, zero last"
        def list = ['abc 001', 'abc 1', 'abc 01', 'abc 01']
        Collections.shuffle(list)
        Collections.sort(list, VersionsComparator.comparingVersions(false))

        then: "order correct"
        list == ['abc 1', 'abc 01', 'abc 01', 'abc 001']

        when: "sorting alpha-numeric string, zero first"
        list = ['abc 001', 'abc 1', 'abc 01', 'abc 01']
        Collections.shuffle(list)
        Collections.sort(list, VersionsComparator.comparingVersions(true))

        then: "order correct"
        list == ['abc 001', 'abc 01', 'abc 01', 'abc 1']

    }

    def "Check versions sorting"() {

        List<String> sorted = ['1.0', '1.3', '1.31', '2.0', '2.1', '2.1-rc1', '2.1-rc2', '2.1-rc12', '2.1.1', '2.1.2', '2.1.12']

        when: "sorting versions"
        def list = new ArrayList(sorted)
        Collections.shuffle(list)
        Collections.sort(list, VersionsComparator.comparingVersions(false))

        then: "sorted"
        list == sorted
    }
}
