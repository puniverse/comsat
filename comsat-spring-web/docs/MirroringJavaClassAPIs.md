The following reasonings are largely mechanical, so they could be automated and packaged into an assisted integration tool.

Mirroring class-based Java APIs (compromising between maximum fidelity and maximum implementation reuse)
========================================================================================================

In Comsat, the goal of mirroring a class-based API (or hierarchy) is to provide an as-smooth-as-possible API porting path from thread-blocking to
fiber-blocking.
One particular case of API mirroring has to be carried out for a whole class hierarchy because the original API is class-based (as opposed to interface-based)
and Java is a single-inheritance language.
This is precisely the case of servlet-based integrations (and possibly other, if not all, container-managed APIs). In this latter case, fiber-blocking HTTP
servlets must not extend `javax.servlet.HttpServlet` but instead the corresponding `co.paralleluniverse.fibers.servlet.FiberHttpServlet`.

Let `R` mirroring `O` be as follows:

```
<modifiers> class O(riginal) extends O(riginal)B(ase) implements I1 ... In { C1 ... Cn, F1 ... Fn, M1 ... Mn }
<modifiers> class R(eplacement) extends R(eplacement)B(ase) implements I1 ... In { <...TBD...> }
```

...where it is safe to assume that `RB` already mirrors `OB` (else the following reasoning can recursively be applied up-hierarchy until a common ancestor is
reached).

In order to mirror a class', its body must be mirrored. Mirroring means either _proxying_ or _re-implementing_ all non-private _features_ (with preference
for proxying when possible, as it's generally less expensive). Class features are _fields_, _methods_ and _inner classes_.

Need for re-implementing
------------------------

A feature in `O` _needs to be re-implemented_ in `R` iff at least one of the following conditions is met:

1. It must be re-implemented because the old implementation is inadequate; some Quasar/Comsat specific notes:
   * There's no need for re-implementation when it just needs to be marked for fiber instrumentation at its call sites, as `META-INF/suspendables` and
     `META-INF/suspendable-supers` can be used

2. It depends on features of `O` (or its base classes) that have been re-implemented in `R` and it's not possible to _forward_ its access to them; _forwarding_
   is defined later on
3. Features already re-implemented in `R` depend on it and can't _access_ it, not even through an intermediate _proxy_; _access_ is defined later on
4. It doesn't strictly _needs_ to be re-implemented but is better to do so for practical reasons, for example:
   * Because _proxying_ or _forwarding it_ would require more code and no additional re-implementation is implied
   * Because re-implementation is almost full already and completing it would allow fully mirroring construction API as well (see last section)

Forwarding references
---------------------

- References through `super` can't be forwarded (they are not polymorphic)
- Field references can't be forwarded (they are not polymorphic)
- Class references can't be forwarded
- A method reference can be forwarded through instrumentation iff:
  - It is acceptable to redefine the class containing the referenced feature (it can't be done for single instances)
- A Method reference can be forwarded through subclassing iff all of the following hold:
  - The class hosting the method is not final
  - The class hosting the method is not private
  - The method is not final
  - The method is not private
  - Creation of the instance defining the method can be performed with the subclass runtime type

In general, in order to support subclassing a mirrored hierarchy, a forwarding subclass should be provided (or runtime-generated) for each mirrored class,
which will be wrapped for proxying in new subclasses of the corresponding mirror class.

Proxying
--------

Proxying means providing a _proxy_, that is features whose sole purpose is to allow access to features of another instance.
Proxy rules are exactly the same as access rules.

Accessing
---------

A prerequisite for direct access to features of an instance (either by the mirroring class or by an intermediate proxy) is holding an instance reference.

- Private features can't be accessed
- Package features can be accessed iff the accessor is located in the same package
- Protected features can be accessed iff either
  - The accessor is located in the same package
  - The accessor inherits from the class hosting the feature to be accessed, but still can access only protected features with own instance reference (and not
    others')
- Public features can always be accessed

Mirroring Java constructor APIs
-------------------------------

Here are some constructor APIs facts:

- Public and package constructors of non-`abstract` classes are part of the user APIs and must be mirrored as well
- Public constructors of `abstract` classes and protected constructors are part of the extension APIs and must be mirrored as well, at least for non-`final`
  classes intended to be extension points (Java does nost support closed inheritance hiearchies but such classes could be named "non-closed")

But there are conflicting facts as well:

- Proxying requires delegating to an instance belonging to the original class' (or a subclass)
- Method forwarding requires either:
  - Instrumenting the whole original class (not nice as it forbids using old and new APIs in the same JVM AFAIK, so not the approach being used)
  - Delegating to a purposedly-built instance of a purposedly-built extension of the original class

Thus, perfect constructor APIs mirroring is possible only when either:

- No forwarding and no proxying is done (which means that full API re-implementation is being performed)
- The delegation target needs not being an existing instance and can be purposedly built

The latter fact is true only when the class-based API being mirored is not `abstract`. This means that:

1. `abstract` classes are extension-only APIs. For this reason, `abstract` classes are correspondingly mirrored by other `abstract` classes. If the replicas
   implementations use proxying or forwarding they must be constructed on pre-built delegate instances (they cannot build concrete ones as they are `abstract`,
   being extension-only APIs). This means they cannot perfectly replicate (extension-only) contructor APIs unless they avoid proxying and forwarding (in which
   case they are full re-implementations). This might not a best compromise between mirroring fidelity and reuse, so it might more convenient for them to
   support only a _wrapping_ construction API.
2. "Open" concrete classes should support both the the original constructor APIs (in case they are _used_ and can build the corresponding mirrored class
   instance themselves) and a wrapping constructor
   API (in case they are _extended_: this use case is same as the `abstract` case so the same reasoning and policies apply)
3. "Closed" or `final` concrete classes can't be extended and should only mirror original constructor API
4. In case of full re-implementation the original constructor API can be mirrored