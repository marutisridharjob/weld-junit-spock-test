# Weld JUnit Extensions

[![Travis CI Build Status](https://img.shields.io/travis/weld/weld-junit/master.svg)](https://travis-ci.org/weld/weld-junit)
[![Maven Central](http://img.shields.io/maven-central/v/org.jboss.weld/weld-junit4.svg)](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22weld-junit4%22)
[![License](https://img.shields.io/badge/license-Apache%20License%202.0-yellow.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

The primary goal of this project is to provide simple and fast tools for CDI *unit/component* testing.
The tools are implemented as JUnit 4 and JUnit 5 extensions.
Supports Weld **2.4** (CDI 1.2) and **3.0** (CDI 2.0).

## The What

Weld JUnit extensions allow you to write unit tests for your beans regardless of the target environment (Java SE, Java EE, etc.).
It starts a real CDI container in minimal configuration meaning you can leverage all bean capabilities - injection, interception, events, etc. - without the need for mocking.
These extensions boot up CDI container before each test container run and shut it down afterwards.
You have the power to customize what beans, extensions, interceptors (and so on) are going to be in the container.
Furthermore, you can `@Inject` directly in your test class - and the list goes on...


## The Why

As a matter of fact, one could have a unit test for CDI bean without running a container, but that would present quite a few drawbacks.
Simulating field injection to start with, then interceptors and/or decorators - all in all, quite a challenge.
There are frameworks to make this easier such as [Mockito](http://site.mockito.org/); but use too many mocks and things get tangled real quick.
So we came with JUnit extensions which allow you to use actual CDI container instead of complex simulations.
There is no need to change the way you develop your CDI components if you have a real container to test it with.
Besides, it's easy to combine this approach with mocking frameworks (see also [Adding mock beans](#adding-mock-beans)).

## How To Use Each Extension

This project consists of three modules.
Below is a list with links to detailed README of each extension:

* JUnit 4 extension
  * [JUnit 4 extension using `@Rule` mechanism](junit4/README.md)
* JUnit 5
  * [JUnit 5 extension using the `@ExtendWith` mechanism](junit5/README.md)
* JUnit-common
  * Houses the parts of code shared by both extensions
