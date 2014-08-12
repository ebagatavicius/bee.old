Eclipse 4.X instructions

---------- Code style/formatting ----------

Window->Preferences->Java->Code Style->Formatter->Import...
  code-style/bee-format.xml

Window->Preferences->JavaScript->Code Style->Formatter->Import...
  code-style/bee-js-format.xml

----------- Import organization -----------

Window->Preferences->Java->Code Style->Organize Imports->Import...
  code-style/gwt.importorder
  
Number of static imports needed for .* = 0

------------ Member sort order ------------

Window->Preferences->Java->Appearance->Members Sort Order
There is no import here, so make your settings match:
  code-style/gwt-sort-order.png

First, members should be sorted by category.
1) Types
2) Static Fields
3) Static Initializers
4) Static Methods
5) Fields
6) Initializers
7) Constructors
8) Methods

Second, members in the same category should be sorted by visibility.
1) Public
2) Protected
3) Default
4) Private

Third, within a category/visibility combination, members should be sorted alphabetically.
 
------- Compiler errors & warnings --------

File->Import->General->Preferences...
  code-style/compiler-preferences.epf

== Checkstyle ==

Checkstyle is used to enforce good programming style.

1. Install Eclipse Checkstyle plugin

The Eclipse Checkstyle plugin can be found at:
  http://eclipse-cs.sourceforge.net/

2. Import bee checks:

Window->Preferences->Checkstyle->New...
Set the Type to "External Configuration File"
Set the Name to "bee checks"
Set the Location to "code-style/bee-checkstyle.xml".

3. Configure project:

Project->Properties->Checkstyle...
Uncheck "Use simple configuration"
Check "Checkstyle active for this project"
Activate default File Set
Click "Edit"

Set the Check Configuration to "bee checks"
Set the Regular expression patterns to:
src/com/.*java
src/.*properties
test/.*java

== todo ==

1. Checkstyle declaration order check is incompatible with gwt member sort order.
2. Elemental library import order.
   


