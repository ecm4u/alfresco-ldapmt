current version: 1.0.0-SNAPSHOT

# Alfresco LDAP Multi-Tenancy Subsystem

This Alfresco module provides a subsystem that makes it possible to use LDAP/AD
authentication and synchronization of authorities while multi-tenancy is
enabled.

## Supported Alfresco Version

This module is build for and tested with Alfresco 6.1.

## Build

To build this module Java 8+ and Maven are required. Clone this repository,
enter the cloned directory. Then build the module:

```bash
mvn package
```

This will create the module as `target/ldapmt-{version}.jar`.

## Installation

Copy the module JAR file into the directory `module/platform` of your Alfresco
installation. After configuring the module, restart Alfresco.

## Configuration

### Create Subsystems

Create an authentication subsystem:

```
alfresco/extension/subsystems
└── Authentication
    └── ldapmt
        └── ldapmt1
            └── ldapmt-authentication.properties
```

Create a synchronization subsystem:

```
alfresco/extension/subsystems
└── Synchronization
    └── default
        └── default
            └── ldapmt-synchronization.properties
```

### Configure Authentication Chain

To use the subsystem in the authentication chain, add a `ldapmt` instance to it.

Example:

```properties
authentication.chain=alfrescoNtlm1:alfrescoNtlm,ldapmt1:ldapmt
```

**Important:** Note that the name of the `ldapmt` instance (`ldapmt1` in the
example above) determines the Zone into which authorities are synchronized. If
you change this name, authorities will be deleted and created as new
authorities!

### Configuration Properties

This subsystems uses properties similar to the usual synchronization and
authorization properties. The property names use the prefix `ldapmt` instead of
`ldap`.

**Important:** If you already have synchronization and authorization properties
configured, note that this module uses **different** property names. The prefix
`ldapmt` is required. Properties with the prefix `ldap` have no effect for this
module!

Templates for the property files are included here as
[`ldapmt-authentication.properties`](src/main/resources/alfresco/subsystems/Authentication/ldapmt/ldapmt-authentication.properties)
and [`ldapmt-synchronization.properties`](src/main/resources/alfresco/subsystems/Synchronization/default/ldapmt-synchronization.properties).

#### Properties with Tenant Placeholders

This subsystem assumes that authorities are organized by tenant in the LDAP/AD.
If the authorities of the tenant `tenant` are to be synchronized, the LDAP
queries are constructed using `tenant` and templates. This template mechanism
applies to two configuration properties:

* `ldapmt.synchronization.personQuery`
* `ldapmt.synchronization.personDifferentialQuery`

The default values for these properties are

```properties
ldapmt.synchronization.personQuery=(&(objectclass=inetOrgPerson)(memberOf=CN={TENANT}_USERS,o={tenant},DC=example,DC=com))
ldapmt.synchronization.personDifferentialQuery=(&(objectclass=inetOrgPerson)(!(modifyTimestamp<={0}))(memberOf=CN={TENANT}_USERS,o={tenant},dc=example,dc=com))
```

To construct correct LDAP queries, the name of the tenant to synchronize is
replaced inside the property values. The placeholder `{tenant}` is replaced
with the name of the tenant, the placeholder `{TENANT}` is replaced with the
name of the tenant in upper case.

*Example:*

The tenant name `tenant1` used together with the two default property values
results in these effective values:

* `(&(objectclass=inetOrgPerson)(memberOf=CN=TENANT1_USERS,o=tenant1,DC=example,DC=com))`
* `(&(objectclass=inetOrgPerson)(!(modifyTimestamp<={0}))(memberOf=CN=TENANT1_USERS,o=tenant1s,dc=example,dc=com))`

## License

This module is free software: you can redistribute it and/or modify it under
the terms of the GNU Lesser General Public License as published by the Free
Software Foundation, either version 3 of the License, or (at your option) any
later version.
