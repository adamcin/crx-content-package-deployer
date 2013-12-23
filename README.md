crx-content-package-deployer
============================

Deploys content packages to Adobe Granite applications, like Adobe CQ 5.4, CQ 5.5, and AEM 5.6

Features
--------

1. Supports password-less HTTP Signature authentication using your configured SSH Private Keys, eliminating the need to
coordinate password exchange between developers, devops, and operations teams.

1. Downloads and/or deploys 1-to-many CRX packages per execution, and deploys each CRX package to 1-to-many servers per
execution

1. The multiselect-enabled Package Choice Parameter allows for execution of parameterized builds using a selection
widget populated with a list of packages retrieved from a CRX server.
