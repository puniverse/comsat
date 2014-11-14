Mimicking Java APIs with best compromise between maximal fidelity and maximal implementation reuse
==================================================================================================

In Comsat, the goal of mimicking an API (or hierarchy) is to provide an as-smooth-as-possible API porting path from thread-blocking to fiber-blocking.
One particular case of API mimicking is when it has to be carried out for a whole class hierarchy because the original API is class-based (as opposed to interface-based) and Java is a
single-class-inheritance language, that is mostly made of (possibly `abstract`) classes that extend others.
This is precisely the case of servlet-based integrations (and possibly other, if not all, container-managed APIs). In this latter case, fiber-blocking HTTP servlets must not extend
`javax.servlet.HttpServlet` but instead the corresponding `co.paralleluniverse.fibers.servlet.FiberHttpServlet`.

Let `R` mimicking `O` be as follows:

```
<modifiers> class O(riginal) extends O(riginal)B(ase) implements I1 ... In { C1 ... Cn, F1 ... Fn, M1 ... Mn }
<modifiers> class R(eplacement) extends R(eplacement)B(ase) implements I1 ... In { <...TBD...> }
```

...where it is safe to assume that `RB` already mimicks `OB` (else the following reasoning can recursively be applied up-hierarchy until a common ancestor is reached).

In order to mimicking a class', its body must be mimicked. Mimicking means either _proxying_ or _rewriting_ all non-private _features_ (with preference for proxying when possible, as it's
generally less expensive). Class features are _fields_, _methods_ and _inner classes_.

Need for rewriting
------------------

A feature in `O` _needs to be rewritten_ in `R` iff at least one of the following conditions is met:

0. It is better to rewrite it because proxying it requires more code and no additionl rewriting is implied
1. It must be changed because the old implementation is inadequate
2. It depends on features of `O` that have been rewritten in `R` and it's not possible to _forward_ its access to them; _forwarding_ is defined later on
3. Features already rewritten in `R` depend on it and can't _access_ it, not even through an intermediate _proxy_; _access_ is defined later on

Forwarding references
---------------------

- Field references can't be forwarded
- Class references can't be forwarded
- A method reference can be forwarded through instrumentation iff:
  - It is acceptable to redefine the class hosting the referenced feature at runtime (it can't be done for single instances)
- A Method reference can be forwarded through subclassing iff
  - The class hosting the method is not final
  - The class hosting the method is not private
  - The method is not final
  - The method is not private
  - Creation of the instance hosting the method can be performed with the subclass runtime type

Proxying
--------

Proxying means providing a _proxy_, that is features whose sole purpose is to allow access to features of another instance.
Proxy rules are exactly the same as access rules.

In the context of mimicking, a feature that doesn't need to be rewritten must be proxied.

Accessing
---------

A prerequisite for direct access to features of an instance (either by the mimicking class or by an intermediate proxy) is holding an instance reference.

- Private features can't be accessed
- Package features can be accessed iff the accessor is located in the same package
- Protected features can be accessed iff either
  - The accessor is located in the same package
  - The accessor inherits from the class hosting the feature to be accessed, but still can access only protected features with own instance reference (and not others')
- Public features can always be accessed

Mimicking Java constructor APIs
-------------------------------

Here are some constructor APIs facts:

- Public and package constructors of non-`abstract` classes are part of the user APIs and must be mimicked as well
- Public constructors of `abstract` classes and protected constructors are part of the extension APIs and must be mimicked as well, at least for non-`final` classes intended to be extension points
(Java doesn't support closed inheritance hiearchies but such classes could be named "non-closed")

But there are conflicting facts as well:

- Proxying requires delegating to an instance belonging to the original class' (or a subclass)
- Method forwarding requires either:
  - Instrumenting the whole original class (not nice as it forbids using old and new APIs in the same JVM AFAIK, so not the approach being used)
  - Delegating to a purposedly-built instance of a purposedly-built extension of the original class

Thus, perfect constructor APIs mimicking is possible only when either:

- No forwarding and no proxying is done (which means that full API rewriting is being carried out)
- The delegation target needs not being an existing instance and can be purposedly built

The latter fact is true only when the class-based API being mimicked is not `abstract`. This means that:

a. `abstract` classes are extension-only APIs. For this reason, `abstract` classes are correspondingly mimicked by other `abstract` classes. If the replicas implementations use proxying or forwarding
they must be constructed on pre-built delegate instances (they cannot build concrete ones as they are `abstract`, being extension-only APIs). This means they cannot perfectly replicate
(extension-only) contructor APIs unless they avoid proxying not forwarding, which means they are full rewrites
This might not a best compromise between mimicking fidelity and reuse, so it might more convenient for them to support only a _wrapping_ construction API
b. "Open" concrete classes should support both the the original constructor APIs (in case they are _used_ and can build the corresponding mimicked class instance themselves) and a wrapping constructor
API (in case they are _extended_: this use case is same as the `abstract` case so the same reasoning and policies apply)
c. "Closed" or `final` concrete classes can't be extended and should only mimick original constructor API
