[![pipeline status](https://gitlab.com/mvysny/jdbi-orm/badges/master/pipeline.svg)](https://gitlab.com/mvysny/jdbi-orm/commits/master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.gitlab.mvysny.jdbiorm/jdbi-orm/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.gitlab.mvysny.jdbiorm/jdbi-orm)

# JDBI-ORM database mapping library

`jdbi-orm` allows you to load the data from database rows into objects (POJOs)
and write the data back into the database. No JPA dirty tricks are used: no runtime
enhancements, no lazy loading, no `DetachedExceptions`, no change tracking
behind the scenes - everything happens explicitly. No compiler
plugin is needed - `jdbi-orm` uses Java language features to add a standard
set of finders to your entities. You can add any number of business logic methods as
you like to your entities; the database transaction is easy to launch simply by calling the
global `jdbi().inTransaction(handle -> { ... });` function.

See [JDBI](http://jdbi.org) for more information.

No dependency injection framework is required - the library works in all
sorts of environments, including a pure JVM project with just a `main()` method.
The library requires Java 11 or higher to work. No Maven compiler
plugin needed - only Java 11 language features are used. That's why no IDE plugin
is needed, since we're only using Java 11 language features.

```
"Simplicity is prerequisite for reliability."
    - Edsger Dijkstra
```

> This library is tailored for Java usage. For Kotlin bindings please visit
[vok-orm](https://github.com/mvysny/vok-orm).

## Supported Databases

jdbi-orm currently supports MySQL, MariaDB, PostgreSQL, H2, Microsoft SQL and CockroachDB.
Other databases are untested - they might or might not work.

## Usage

Just add the following lines to your Gradle script, to include this library in your project:
```groovy
repositories {
    mavenCentral()
}
dependencies {
    compile("com.gitlab.mvysny.jdbiorm:jdbi-orm:x.y")
}
```

> Note: obtain the newest version from the tag name at the top of the page

Maven: (it's very simple since jdbi-orm is in Maven Central):

```xml
<project>
	<dependencies>
		<dependency>
			<groupId>com.gitlab.mvysny.jdbiorm</groupId>
			<artifactId>jdbi-orm</artifactId>
			<version>x.y</version>
		</dependency>
    </dependencies>
</project>
```

Compatibility Chart:

| jdbi-orm version | validation API     | Min. Java version |
|------------------|--------------------|-------------------|
| 0.x              | javax.validation   | 8+                |
| 1.x              | jakarta.validation | 11+               |

### Trying out/quickstart/main

See the [jdbi-orm-playground](https://gitlab.com/mvysny/jdbi-orm-playground) project which introduces
a `main()` method, a single `Person` entity and an in-memory H2 database.
Perfect for experimenting with `jdbi-orm` since you only need Java JRE to run it - no containers nor setup needed.
See [JDBI-ORM Vaadin CRUD demo](https://github.com/mvysny/jdbi-orm-vaadin-crud-demo)
for a CRUD demo web app using `jdbi-orm`.

## Usage Examples

Say that we have a table containing a list of beverage categories, such as Cider or Beer. The H2 DDL for such table is simple:

```sql92
create TABLE CATEGORY (
  id bigint auto_increment PRIMARY KEY,
  name varchar(200) NOT NULL
);
create UNIQUE INDEX idx_category_name ON CATEGORY(name);
```

> **Note:** We expect that the programmer wants to write the DDL scripts herself, to make full
use of the DDL capabilities of the underlying database.
We will therefore not hide the DDL behind some type-safe generator API.

Such entity can be mapped to a Java class as follows:
```java
public class Category implements Entity<Long> {
    private Long id;
    private String name;

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
```
(the `id` is a nullable `Long` since its value is initially `null` until the category
is actually created and the id is assigned by the database).

The `Category` class is just a simple data class: there are no hidden private fields added by
runtime enhancements, no hidden lazy loading - everything is pre-fetched upfront. Because of that,
the class can be passed around the application freely as a DTO (data transfer object),
without the fear of failing with
`DetachedException` when accessing properties. Since `Entity` is `Serializable`, you can
also store the entity into a session. 

> The Category class (or any entity class for that matter) must have a zero-arg constructor.
Zero-arg constructor is mandated by JDBI, in order for JDBI to be able to construct
instances of entity class for every row returned. It's possible to use `ConstructorMapper`
to construct any entities; please consult [JDBI Documentation](http://jdbi.org/) for more
details.

By implementing the `Entity<Long>` interface, we are telling jdbi-orm that the primary key is of type `Long`;
this will be important later on when using DAO.
The [Entity](src/main/java/com/gitlab/mvysny/jdbiorm/Entity.java) interface brings 
in the following useful methods:

* `save()` which either creates a new row by generating the INSERT statement (if the ID is null),
   or updates the row by generating the UPDATE statement (if the ID is not null)
* `create()` for special cases when the ID is pre-known (social security number) and
   `save()` wouldn't work. More info in the 'Pre-known IDs' chapter.
* `delete()` which deletes the row identified by the `id` primary key from the database.
* `validate()` validates the bean. By default all `javax.validation` annotations are 
  validated; you can override this method to provide further bean-level validations.
  Please read the 'Validation' chapter below, for further details.

The INSERT/UPDATE SQL statement is automatically constructed by the `save()` method, 
simply by enumerating all non-transient and non-ignored properties of
the bean using reflection and fetching their values. See the 
[Entity](src/main/java/com/gitlab/mvysny/jdbiorm/Entity.java) sources for more details.
You can annotate the `Category` class with the `@Table(dbname = "Categories")`
annotation, to specify a different table name.

The category can now be created easily:

```java
Category cat = new Category();
cat.setName("Beer");
cat.save();
```

But how do we specify the target database where to store the category in? 

### Connecting to a database

To configure jdbi-orm, all you need is to set a `DataSource` to it:

```java
JdbiOrm.setDataSource(dataSource);
```

The simplest way is to
use [Hikari-CP](https://brettwooldridge.github.io/HikariCP/) to create a
`HikariDataSource` out of a `HikariConfig`. Just specify the JDBC URL
to the `HikariConfig` and you're good to go.
HikariCP comes pre-initialized with sensible default settings, but it contains lots of other options as well.

> Hikari-CP is a JDBC connection pool which manages a pool of JDBC connections since they are expensive to create. Typically all projects
use some sort of JDBC connection pooling.

For example, to use an in-memory H2 database, just add both H2 and HikariCP as a dependency to your project:

* `compile("com.h2database:h2:1.4.200")` for H2
* `compile("com.zaxxer:HikariCP:3.4.0")` for HikariCP

Then, configure jdbi-orm as follows:

```java
final HikariConfig hikariConfig = new HikariConfig();
hikariConfig.setJdbcUrl(jdbcUrl);
hikariConfig.setUsername(username);
hikariConfig.setPassword(password);
hikariConfig.setMinimumIdle(0);

JdbiOrm.setDataSource(new HikariDataSource(hikariConfig));
```

Now you can simply call
the `jdbi().withHandle()` function to run the
block in a database transaction. JDBI will acquire new connection from the
connection pool; then it will start a transaction and it will provide you with means to execute SQL commands:

```java
jdbi().withHandle(handle -> handle
        .createUpdate("delete from Category where id = :id")
        .bind("id", 25)
        .execute());
```

You can call this function from anywhere; you don't need to use dependency injection or anything like that.
That is precisely how the `save()` function saves the bean - it simply calls the `jdbi()` function and executes
an appropriate INSERT/UPDATE statement.

The function will automatically roll back the transaction on any exception thrown
out from the block (both checked and unchecked exceptions roll back the transaction).

After you no longer need database access, call `JdbiOrm.destroy()` to close the pool.
The best and easiest way is to simply initialize JdbiOrm via `JdbiOrm.setDataSource()` when your JVM
boots up, and call `JdbiOrm.destroy()` when your server is shutting down (e.g. in VM
shutdown hook), to have easy database access during the entire lifetime of your server.

> Once JdbiOrm is initialized via `JdbiOrm.setDataSource()`, you can call jdbi() or Dao() methods at any time, from any thread.
You don't need to be running inside of the JavaEE or Spring container or
any container at all - you can actually use this library from a plain JavaSE main method.

Full example of a `main()` method that does all of the above:

```java
public class Main {
    public static void main(String[] args) {
        final HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        JdbiOrm.setDataSource(new HikariDataSource(hikariConfig));
        jdbi().withHandle(handle -> handle
                .createUpdate("create TABLE CATEGORY (id bigint auto_increment PRIMARY KEY, name varchar(200) NOT NULL );")
                .execute());
        jdbi().useTransaction(handle -> {
            for (int i = 0; i < 100; i++) {
                final Category cat = new Category();
                cat.setName("cat " + i);
                cat.save();
            }
        });
        JdbiOrm.destroy();
    }
}
```

See the [jdbi-orm-playground](https://gitlab.com/mvysny/jdbi-orm-playground)
project which contains such `main` method, all JDBC drivers and all dependencies pre-loaded, and
simple instructions on how to query different database kinds.

> *Note*: for the sake of simplicity we're running the `CREATE TABLE` as a query. For a persistent database
it's definitely better to use [Flyway](https://flywaydb.org/) as described below.

### Finding Categories

The so-called finder (or DAO, short for Data Access Object) methods actually resemble factory methods since they
also produce instances of Categories. The best place for such
methods is on the `Category` class itself. We can write all of the necessary
finders ourselves, by using the `jdbi()`
method as stated above; however jdbi-orm already provides a set of handy methods for you. All you need
to do is to add a static field to `Category` which creates the `Dao` class:

```java
public class Category implements Entity<Long> {
    private Long id;
    private String name;

    // omitted for brevity: getters & setters

    public static final Dao<Category, Long> dao = new Dao<>(Category.class);
}
```

The Category class will now be outfitted
with several useful finder methods present in the [Dao](src/main/java/com/gitlab/mvysny/jdbiorm/Dao.java) class itself):

* `Category.dao.findAll()` will return a list of all categories
* `Category.dao.getById(25L)` will fetch a category with the ID of 25, failing if there is no such category
* `Category.dao.findById(25L)` will fetch a category with ID of 25, returning `null` if there is no such category
* `Category.dao.deleteAll()` will delete all categories
* `Category.dao.deleteById(42L)` will delete a category with ID of 42
* `Category.dao.count()` will return the number of rows in the Category table.
* `Category.dao.findAllBy("name = :name1 or name = :name2", null, null, q -> q.bind("name1", "Beer").bind("name2", "Cider"))` will find all categories with the name of "Beer" or "Cider".
  This is an example of a parametrized select, from which you only need to provide the WHERE clause.
* `Category.dao.findAllBy(Category.NAME.in("Beer", "Cider"))` uses a type-safe Condition API.
* `Category.dao.singleBy("name = :name", q -> q.bind("name", "Beer"));` will fetch exactly one matching category, failing if there is no such category or there are more than one.
* `Category.dao.findSingleBy("name = :name", q -> q.bind("name", "Beer"));` will fetch one matching category, failing if there are more than one. Returns `null` if there is none.
* `Category.dao.countBy("name = :name", q -> q.bind("name", "Beer"));` will return the number of rows in the Category table matching given query.

In the spirit of type safety, the finder methods will only accept `Long` (or whatever is the type of
the primary key in the `Entity<x>` implementation clause). 

You can of course add your own custom finder methods into the Category companion object. For example:

```java
public class Category implements Entity<Long> {
    private Long id;
    private String name;

    // omitted for brevity: getters & setters

    public static class CategoryDao extends Dao<Category, Long> {

        protected CategoryDao() {
            super(Category.class);
        }

        @Nullable
        public Category findByName(@NotNull String name) {
            return findSingleBy("name = :name", q -> q.bind("name", name));
        }

        @NotNull
        public Category getByName(@NotNull String name) {
            return singleBy("name = :name", q -> q.bind("name", name));
        }

        public boolean existsByName(@NotNull String name) {
            return existsBy("name = :name", q -> q.bind("name", name));
        }
    }
}
```  

> **Note**: If you don't want to use the Entity interface for some reason (for example
when the table has no primary key), you can still include
useful finder methods by making the Dao extend the `DaoOfAny` class.
The ID-related methods such as `findById()` will be missing, but you can still
use all other Dao methods.

### Adding Reviews

Let's add the second table, the "Review" table. The Review table is a list of reviews for
various drinks; it back-references the drink category as a foreign key into the `Category` table:

```sql92
create TABLE REVIEW (
  id bigint auto_increment PRIMARY KEY,
  beverageName VARCHAR(200) not null,
  score TINYINT NOT NULL,
  date DATE not NULL,
  category BIGINT,
  count TINYINT not null
);
alter table Review add CONSTRAINT r_score_range CHECK (score >= 1 and score <= 5);
alter table Review add FOREIGN KEY (category) REFERENCES Category(ID);
alter table Review add CONSTRAINT r_count_range CHECK (count >= 1 and count <= 99);
create INDEX idx_beverage_name ON Review(beverageName);
```

The mapping class is as follows:
```java
/**
 * Represents a beverage review.
 */
public class Review implements Entity<Long> {
    private Long id;
    /**
     * the score, 1..5, 1 being worst, 5 being best
     */
    private Integer score = 1;
    private String beverageName = "";
    /**
     * when the review was done
     */
    private LocalDate date = LocalDate.now();
    /**
     * the beverage category {@link Category#getId()}
     */
    private Long category = null;
    /**
     * times tasted, 1..99
     */
    private Integer count = 1;

    // omitted for brevity: getters & setters

    public static final Dao<Review, Long> dao = new Dao<>(Review.class);
}
```

Now if we want to delete a category, we need to first set the `Review.category` value to `null` for all reviews that
are linked to that very category, otherwise
we will get a foreign constraint violation. It's quite easy: just override the `delete()` method in the
`Category` class as follows:

```java
public class Category implements Entity<Long> {
    // ...
    @Override
    public void delete() {
        jdbi().useHandle(handle -> {
            handle.createUpdate("update Review set category = NULL where category=:catId")
                    .bind("catId", getId())
                    .execute();
        });
        Entity.super.delete();
    }
}
```

> **Note:** for all slightly more complex queries it's a good practice to simply use the JDBI API - we will simply pass in the SQL command as a String to JDBI.

As you can see, you can use the JDBI connection yourself, to execute any kind of SELECT/UPDATE/INSERT/DELETE statements as you like.
For example you can define static finder or computation method into the `Review` companion object:

```java
public class Review implements Entity<Review> {
    // ...

    public static final ReviewDao dao = new ReviewDao();

    public static class ReviewDao extends Dao<Review, Long> {

        protected ReviewDao() {
            super(Review.class);
        }

        /**
         * Computes the total sum of [count] for all reviews belonging to given [categoryId].
         * @return the total sum, 0 or greater.
         */
        public long getTotalCountForReviewsInCategory(long categoryId) {
            final Long count = jdbi().withHandle(handle -> handle
                    .createQuery("select sum(r.count) from Review r where r.category = :catId")
                    .bind("catId", categoryId)
                    .mapTo(Long.class)
                    .one()
            );
            return count == null ? 0 : count;
        }
    }
}
```

Then we can outfit the Category itself with this functionality, by adding a method to the Category class:
```java
    public long findTotalCountForReviews() {
        return Review.dao.getTotalCountForReviewsInCategory(getId());
    }
```

Note how freely and simply we can add useful business logic methods to entities. It's because:

* the entities are just plain old classes with no hidden fields and no runtime enhancements, and
* because we can invoke `jdbi()` freely from anywhere. You don't need transaction annotations and injected entity managers,
  and you don't need huge container such as Spring or JavaEE which must instantiate your classes
  in order to activate those annotations and injections.
  Those are things of the past.

### Auto-generated IDs vs pre-provided IDs
There are generally three cases for entity ID generation:

* IDs generated by the database when the `INSERT` statement is executed
* Natural IDs, such as a NaturalPerson with ID pre-provided by the government (social security number etc).
* IDs created by the application, for example via `UUID.randomUUID()`

The `save()` method is designed to work out-of-the-box only for the first case (IDs auto-generated by the database). In this
case, `save()` emits `INSERT` when the ID is null, and `UPDATE` when the ID is not null.

When the ID is pre-provided, you can only use `save()` method to update a row in the database; using `save()` to create a
row in the database will throw an exception. In order to create an
entity with a pre-provided ID, you need to use the `create()` method:
```java
new NaturalPerson(id = "12345678", name = "Albedo").create()
```

For entities with IDs created by the application you can make `save()` work properly, by overriding the `create()` method
in your entity as follows:
```java
public void create(boolean validate) {
  id = UUID.randomUUID()
  Entity.super.create(validate)
}
```

Even better, you can create a new interface which inherits from the `Entity` interface as follows:

```java
interface UuidEntity extends Entity<UUID> {
    @Override
    default void create(boolean validate) {
        setId(UUID.randomUUID());
        Entity.super.create(validate);
    }
}
```

And simply make your entities implement the `UuidEntity` interface.

### Joins

When we display a list of reviews (say, in a Vaadin Grid), we want to display an actual category name instead of the numeric category ID.
We can take advantage of JDBI simply matching all SELECT column names into bean fields; all we have to do is to:

* extend the `Review` class and add the `categoryName` field which will carry the category name information;
* write a SELECT that will return all of the `Review` fields, and, additionally, the `categoryName` field

Let's thus create a `ReviewWithCategory` class:

```java
public class ReviewWithCategory extends Review {
    @ColumnName("name")
    private String categoryName;

    // omitted for brevity: getters & setters
}
```

> Note the `@ColumnName` annotation which tells jdbi-orm that the field is named differently in the database. Often the database naming schema
is different from Java's naming schema, for example `NUMBER_OF_PETS` would be represented by the `numberOfPets` in the Java class.
You can use database aliases, for example `SELECT NUMBER_OF_PETS AS numberOfPets`. However note that you can't then add a `WHERE` clause on
the `numberOfPets` alias - that's not supported by SQL databases. See [Issue #5](https://github.com/mvysny/vok-orm/issues/5) for more details.
Currently we don't use WHERE in our examples so you're free to use aliases, but aliases do not work with Data Loaders and therefore it's a good
practice to use `@ColumnName` instead of SQL aliases.

Now we can add a new finder function into `Review`'s companion object:

```java
public class ReviewWithCategory extends Review {
    // ...

    public static final ReviewWithCategoryDao dao = new ReviewWithCategoryDao();

    public static class ReviewWithCategoryDao extends Dao<ReviewWithCategory, Long> {
        protected ReviewWithCategoryDao() {
            super(ReviewWithCategory.class);
        }

        public List<ReviewWithCategory> findReviews() {
            return jdbi().withHandle(handle -> handle
                    .createQuery("select r.*, c.name\n" +
                            "FROM Review r left join Category c on r.category = c.id\n" +
                            "ORDER BY c.name")
                    .map(getRowMapper())
                    .list());
        }
    }
}
```

We can take JDBI's mapping capabilities to full use: we can craft any SELECT we want,
and then we can create a holder class that will not be an entity itself, but will merely hold the result of that SELECT.
The only thing that matters is that the class will have properties named exactly as the columns in the SELECT statement (or properly aliased
using the `@ColumnName` annotation):

```java
public class Beverage implements Serializable {
    @ColumnName("beverageName")
    private String name;
    @ColumnName("name")
    private String category;

    // omitted for brevity: getters & setters

    public static List<Beverage> findAll() {
        return jdbi().withHandle(handle -> handle
                .createQuery("select r.beverageName, c.name from Review r left join Category c on r.category = c.id")
                .map(FieldMapper.of(Beverage.class))
                .list());
    }
}
```

We just have to make sure that the `Beverage` has zero-arg constructor otherwise
JDBI will throw an exception.

### Joins Alternative: @Nested

Even better, you can use the `@Nested` annotation; then you don't need to extend the `Review`
class:

```java
public class ReviewWithCategory {
    @Nested
    private Review review;

    @ColumnName("name")
    private String categoryName;

    // omitted for brevity: getters & setters
}
```

Just use the same `ReviewWithCategoryDao` as described above, it will also work with the `@Nested`-annotated
field.

## Controlling The Mapping

When saving, creating or loading from the database, JDBI-ORM simply consults all
non-transient non-static fields (including private ones), using the field name as
the database name. A couple of tips:

* To remove a field from the INSERT/UPDATE/SELECT statements generated by `save()`, `create()`
  or by loading from the database, either mark it with the Java keyword `transient`
  or annotate it with `@JdbiProperty(map = false)`.
* To specify a different database column name simply annotate the field (always field - not the getter/setter) by `@ColumnName`
* Often you need an object that holds all fields of two entities matched by a join, e.g.
  a `PersonDepartment` holding data for `Person` and its `Department`.
  Instead of repeating all `Person` and all `Department` fields in `PersonDepartment`, simply
  annotate the field with the `@Nested` annotation: `@Nested Person person;` and `@Nested Department department;`.
  The field name won't matter anymore; all `Person` fields and all `Department` fields will
  then be populated from the SELECT.

Please read more about the `@Nested` annotation in the [JDBI FieldMapper chapter](http://jdbi.org/#_fieldmapper).
Note that JDBI-ORM honors the `@Nested` annotation also for INSERTs and UPDATEs
generated by the `create()` and `save()` methods.

## One-To-Many, Many-To-Many, Many-to-One Relations

I prefer doing things explicitly, as opposed to Hibernate's dark magic of letting
the `User` have a `Set<Department> getDepartments()` - is the set eager-load or lazy-load?
If lazy-load, what is the paging window size? If I call the getter from outside of a transaction, will it fail with `DetachedException`?

That's why I recommend to manually write all code explicitly. It may be annoying and tedious,
but it will leave you in total control of what gets executed on the database.

For any relation simply add a finder straight into the entity:
```java
class User implements Entity<Long> {
    // ... fields

    public List<Posession> findPosessions() {
        // example for 1-* relation:
        // or even better: move this to a function called Posession.dao.findAllforUser(Long userId)
        return Posession.dao.findAllBy("user_id=:uid", q -> q.bind("uid", getId()));
    }

    public List<Department> findDepartments() {
        // example for *-* relation would need to join through a mapping table department_user.
        // or even better: move this code to the function Department.dao.findAllForUser(Long userId)
        return jdbi().withHandle(handle -> handle
            .createQuery("SELECT d.* FROM department d, department_user du WHERE du.user_id=:uid AND d.id=du.dept_id")
            .bind("uid", getId())
            .map(Department.dao.getRowMapper())
            .list());
    }

    public User getParent() {
        // example of *-1 relation:
        return dao.findById(getParentId());
    }
}
```

A `*-*` relation usually comes with a mapping table: see the "Composite Primary Keys" chapter below for a documentation on
how to represent such a mapping table as an entity with a composite primary key.

Tip: in order to delete rows from the mapping table when e.g. an user is deleted,
simply override the `delete()` function in `User` and delete appropriate rows from the mapping table:

```java
class User implements Entity<Long> {
    // ... fields

    @Override
    public void delete() {
        jdbi().inTransaction(handle -> {
            DepartmentUser.dao.deleteAllForUser(getId());
            Entity.super.delete();
        });
    }

    public static final UserDao dao = new UserDao();

    public static final class UserDao extends Dao<User, Long> {
        private UserDao() {
            super(User.class);
        }

        @Override
        public void deleteAll() {
            jdbi().inTransaction(handle -> {
                DepartmentUser.dao.deleteAll();
                super.deleteAll();
            });
        }
    }
}
```

## Validations

Often the database entities are connected to UI forms which need to provide sensible
validation errors to the users as they enter invalid values. The validation
could be done on the database level, but databases tend to provide unlocalized
cryptic error messages. Also, some validations are either impossible to do, or very hard
to do on the database level. That's why `jdbi-orm` provides additional validation
layer for your entities.

`jdbi-orm` uses [JSR303 Java Standard for Validation](https://en.wikipedia.org/wiki/Bean_Validation); you can
quickly skim over [JSR303 tutorial](https://dzone.com/articles/bean-validation-made-simple) to see how to start
using the validation.
In a nutshell, you annotate your Entity's fields with validation annotations; the fields are
then checked for valid values with the JSR303 Validator (invoked when
`entity.validate()`/`entity.save()`/`entity.create()` is called). The validation is
also mentioned in [Vaadin-on-Kotlin Forms](http://www.vaadinonkotlin.eu/forms.html) documentation.

For example:
```java
public class Person implements Entity<Long> {
    private Long id;
    @Length(min = 1)
    @NotNull
    private String name;
    @NotNull
    @Min(15)
    @Max(100)
    private Integer age;
    // etc
}

new Person("John", 10).validate() // throws an exception since age must be at least 15
```

*Important note:* the validation is an optional feature in `jdbi-orm`, and by default
the validation is disabled. This fact is advertised in the `jdbi-orm` logs as the following message:

> JSR 303 Validator Provider was not found on your classpath, disabling entity validation

In order to activate the entity validations, you need to add a JSR303 Validation Provider jar
to your classpath. Just use Hibernate-Validator (don't worry it will not pull in Hibernate nor
JPA) and add this to your `build.gradle`:

```groovy
dependencies {
  compile("org.hibernate.validator:hibernate-validator:6.0.13.Final")
  // EL is required: http://hibernate.org/validator/documentation/getting-started/
  compile("org.glassfish:javax.el:3.0.1-b08")
}
```

You can check out the [jdbi-orm-playground](https://gitlab.com/mvysny/jdbi-orm-playground) which
has validations enabled and all necessary jars included.

## Listing Subset of Table Columns

Sometimes only a subset of columns is needed to be loaded from a database. Take
a list of images for example: when you display images in a Grid, you definitely
don't want to load the image BLOBs into memory - those should be loaded only
when the image is actually edited or created. This is easy: just create a view and
a bean which inherits from the view:

```java
@Table("images")
public class ImageView implements Entity<Long> {

    private Long id;

    @Length(max = 45)
    @ColumnName("image_name")
    private String imageName;

    @Length(max = 255)
    @ColumnName("image_filename")
    private String imageFileName;

    // omitted for brevity: getters + setters

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ImageView)) return false;
        ImageView imageBank = (ImageView) o;
        return Objects.equals(id, imageBank.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static final Dao<ImageView, Long> dao = new Dao<>(ImageView.class);

    /**
     * Loads a full-blown image, with image data.
     * @return image
     */
    @NotNull
    public ImageBank load() {
        return ImageBank.dao.getById(getId());
    }
}

public final class ImageBank extends ImageView {

    private byte[] image;

    // omitted for brevity: getters + setters

    public static final Dao<ImageBank, Long> dao = new Dao<>(ImageBank.class);
}
```

Now you can find all images via `ImageView.dao.findAll()` without the overhead
of loading BLOBs; you can call `imageView.load()` to get the full-blown entity
with BLOB which can be edited.

## Composite Primary Keys

Using composite primary keys is very easy. Just create a serializable class for the Primary Key,
add the fields to it (optionally annotated by `@ColumName`), then use the `@Nested` annotation as
in the following example:

```sql
create table mapping_table(person_id bigint not null, department_id bigint not null, some_data varchar(400) not null, PRIMARY KEY(person_id, department_id))
```
```java
@Table("mapping_table")
public final class MappingTable implements Entity<MappingTable.ID> {

    public static final class ID implements Serializable {
        @ColumnName("person_id")
        public Long personId;
        @ColumnName("department_id")
        public Long departmentId;

        // hashCode/equals/toString() omitted for brevity
    }

    // the field needs to be named "id"
    @Nested
    private ID id;

    @ColumnName("some_data")
    private String someData;

    // getters, setters, equals, hashCode, toString() omitted for brevity

    public static final Dao<MappingTable, ID> dao = new Dao<>(MappingTable.class);
}
```

## Data Loaders

This library does not support integration with [Data Loaders](https://gitlab.com/mvysny/vok-dataloader)
(since `vok-dataloader` is primarily intended to be used from Kotlin project)
nor Vaadin 8/14 Grid directly. However, the Kotlin bindings do, please read more at
[vok-orm](https://github.com/mvysny/vok-orm).

There's an example project in Java which uses `vok-orm` and `vaadin-on-kotlin` to show `jdbi-orm` Java entities
in a Grid: [Vaadin 8: vaadin8-sqldataprovider-example](https://github.com/mvysny/vaadin8-sqldataprovider-example)
and [Vaadin 14: vaadin10-sqldataprovider-example](https://github.com/mvysny/vaadin10-sqldataprovider-example)
Despite its name, the project demoes both SQLDataProvider and EntityDataProvider.

Since jdbi-orm 2.0, there is a support for creating WHERE conditions programmatically, which
is very handy for Data Providers. See below for more information.

## Condition API

The Condition API offers a programmatic way to create WHERE clauses; since jdbi-orm 2.0.
This is very handy for use with Data Providers, but also for creating simple WHERE selects quickly and easily.
First, you need to add column definitions to your entities:

```java
public class Category implements Entity<Long> {
  private Long id;
  private String name;
  // getters+setters omitted

  @JdbiProperty(map = false)
  public static final TableProperty<Category, Long> ID = TableProperty.of<>(Category.class, "id");
  @JdbiProperty(map = false)
  public static final TableProperty<Category, String> NAME = TableProperty.of<>(Category.class, "name");
}
```

You need to do that for all of your database columns. This will allow you to create the Condition
which can then be passed to your DAO:

```java
List<Category> categories = Category.dao.findAllBy(Category.NAME.like("Beer%").and(Category.ID.gt(2)));
```

## Aliases

Often database columns follow different naming convention than bean fields, e.g. database `CUSTOMER_NAME` should be mapped to the
`CustomerAddress::customerName` field. The first thing to try is to use aliases in the SQL itself, for example
```sql
select c.CUSTOMER_NAME as customerName from Customer c ...;
```

The problem with this approach is twofold:

* Databases can't sort nor filter based on aliased column; please see [Issue 5](https://github.com/mvysny/vok-orm/issues/5) for more details.
  Using such queries with `SqlDataLoader` and trying to pass in filter such as `buildFilter<CustomerAddress> { "customerName ILIKE cn"("cn" to "Foo%") }` will cause
  the select command to fail in the database.
* INSERTs/UPDATEs issued by your entity `Dao` will fail since they will use the bean field names instead of actual column name
  and will emit `INSERT INTO Customer (customerName) values ($1)` instead of `INSERT INTO Customer (CUSTOMER_NAME) values ($1)` 

Therefore, instead of database-based aliases it's better to use the `@ColumnName` annotation on your beans, both natural entities
such as `Customer` and projection-only entities such as `CustomerAddress`:

```java
public final class Customer implements Entity<Long> {
    @ColumnName("CUSTOMER_NAME")
    private String name;
}
public final class CustomerAddress {
    @ColumnName("CUSTOMER_NAME")
    private String customerName;
}
```

The `@ColumnName` annotation is honored both by `Dao`s and by all data loaders.

## group by/aggregates/pivot table

Since the group-by query may produce data with different data types than the bean itself contains (e.g.
averaging integer age may produce a float/double result), it's best to create a new class to hold the
outcome of the query:

```java
public final class ReviewAvgScore implements Serializable {
    @ColumnName("beverageName")
    private String name;
    private float avgScore;
    // getters and setters

    public static List<ReviewAvgScore> findAll() {
        return jdbi().withHandle(handle -> handle
                .createQuery("select beverageName, avg(score) as avgScore from Review group by beverageName order by beverageName")
                .map(FieldMapper.of(ReviewAvgScore.class))
                .list());
    }
}
```

Note that we're not using the `Dao` here since the class is not backed by a table.

Note that this approach is not fit for a configurable pivot table which may need to
support dynamic criteria list. For that I'd recommend to:

1. create the SQL depending on the grouping/aggregate criteria list;
2. Use a custom JDBI Mapper, to map the JDBC rows into some kind of dynamic row,
   e.g. backed by a `HashMap`.

## A main() method Example

Using the vok-orm library from a JavaSE main method;
see the [jdbi-orm-playground](https://gitlab.com/mvysny/jdbi-orm-playground) for a very simple example project
using `jdbi-orm`.

```java
public class Category implements Entity<Long> {
    private Long id;
    private String name;

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long findTotalCountForReviews() {
        return Review.dao.getTotalCountForReviewsInCategory(getId());
    }

    public static final CategoryDao dao = new CategoryDao();

    public static class CategoryDao extends Dao<Category, Long> {

        public CategoryDao() {
            super(Category.class);
        }

        @Nullable
        public Category findByName(@NotNull String name) {
            return findSingleBy("name=:name", q -> q.bind("name", name));
        }
    }

    public static void main(String[] args) {
        final HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        JdbiOrm.setDataSource(new HikariDataSource(hikariConfig));
        jdbi().useHandle(handle -> handle
                .createUpdate("create TABLE CATEGORY (id bigint auto_increment PRIMARY KEY, name varchar(200) NOT NULL );")
                .execute());

        // runs SELECT * FROM Category
        // prints []
        System.out.println(Category.dao.findAll());

        // runs INSERT INTO Category (name) values (:name)
        final Category category = new Category();
        category.setName("my category");
        category.save();

        // runs SELECT * FROM Category where id=:id
        // prints "my category"
        System.out.println(Category.dao.getById(1L).getName());

        // mass-saves 10 categories in a single transaction.
        jdbi().useTransaction(handle -> {
            for (int i = 0; i < 10; i++) {
                final Category cat = new Category();
                cat.setName("my category " + i);
                cat.save();
            }
        });

        JdbiOrm.destroy();
    }
}
```

# Using Flyway to migrate the database

[Flyway](https://flywaydb.org/) is able to run DDL scripts on given database and track which scripts already ran.
This way, you can simply add more scripts and Flyway will apply them, to migrate the database to the newest version.
This even works in a cluster since Flyway will obtain a database lock, locking out other members of the cluster attempting
to upgrade.

Let's use the Category example from above. We need Flyway to run two scripts, to initialize the database:
one creates the table, while other creates the indices.

You don't need to use Flyway plugin. Just add the following Gradle dependency to your project:

```gradle
implementation("org.flywaydb:flyway-core:9.8.1")
```

Flyway expects the migration scripts named in a certain format, to know the order in which to execute them.
Create the `db/migration` folder in your `src/main/resources` and put two files there: the `V01__CreateCategory.sql`
file:
```sql92
create TABLE CATEGORY (
  id bigint auto_increment PRIMARY KEY,
  name varchar(200) NOT NULL
);
```
The next one will be `V02__CreateIndexCategoryName.sql`:
```sql92
create UNIQUE INDEX idx_category_name ON CATEGORY(name);
```

In order to run the migrations, just run the following after a datasource is set to `JdbiOrm`:
```java
final Flyway flyway = Flyway.configure()
  .dataSource(JdbiOrm.getDataSource())
  .load();
flyway.migrate();
```

See the [jdbi-orm-vaadin-crud-demo](https://github.com/mvysny/jdbi-orm-vaadin-crud-demo) for an example.

# Using with Spring or JavaEE

Very easy: all you need to do is just pass the `DataSource` instance to `JdbiOrm.setDataSource()`
when your server boots up. The easiest way to achieve that is to create a singleton bean which
is instantiated upon server start, then inject a `DataSource` to the bean and
call `JdbiOrm.setDataSource()` in the bean's `@PostConstruct`-annotated method.

Since Spring or JavaEE containers manage
data source instances for you, in such setup you must not call `JdbiOrm.destroy()`.
The `JdbiOrm.destroy()` is supposed to be called only if you have created the `DataSource` yourself.

# `jdbi-orm` design principles

`jdbi-orm` is a very simple object-relational mapping library, built around the following ideas:

* Simplicity is the most valued property
* Working with plain SQL commands is preferred over having a type-safe
  query Java API. However, having a query API is handy to have for Data Providers,
  and therefore jdbi-orm provides a basic Condition API (since jdbi-orm 2.0).
* The database is the source of truth. JVM objects are nothing more than DTOs,
  merely capturing temporary snapshots of the JDBC `ResultSet` rows. The entities are populated by the
  means of reflection: for every column in
  the JDBC `ResultSet` an appropriate setter is invoked, to populate the data.
* The entities are real POJOs: they do not track modifications, they do not automatically store modified
  values back into the database. They are not runtime-enhanced and can be final.
* A switch from one type of database to another never happens. We understand that the programmer
  wants to exploit the full potential of the database, by writing SQLs tailored for that particular database.
  `jdbi-orm` should not attempt to generate SELECTs on behalf of the programmer (except for the very basic ones related to CRUD);
  instead it should simply allow SELECTs to be passed as Strings, and then map the result
  to an object of programmer's choosing.
* The entities are hand-written, tailored towards your needs, committed in git and they
  are never overwritten by a code generator. The downside is that you need to write them
  by hand, there is no code generator to generate those.

Please read [Back to Base - make SQL great again](http://mavi.logdown.com/posts/5771422)
for the complete explanation of ideas behind this framework.

This framework uses [JDBI](http://jdbi.org/) to map data from the JDBC `ResultSet` to POJOs; in addition it provides a very simple
mechanism to store/update the data back to the database.

## Comparison with other database-related libraries

* [ActiveJDBC](https://javalite.io/activejdbc) has much in common with jdbi-orm; the advantage of jdbi-orm
  is that we do not require any instrumentation to work (we use only Java language features).
* [JOOQ](https://www.jooq.org/) is great but requires initial generation of java code from your database scheme
  (opposed to that, you write your entities by hand with jdbi-orm and you don't need to run any generator),
  and promotes type-safe query building instead of plain SQLs.
  There's the usual set of problems coming with generated classes: you can't add your custom utility functions to those,
  you can't add validation annotations, etc.
  If you don't mind that, go for JOOQ - it's definitely more popular than jdbi-orm.
* JPA: just no. We want real POJOs, not a dynamically-enhanced thing managed by the Entity Manager. Also see below.
* Spring JdbcTemplate: not bad but it depends on Spring; jdbi-orm must be able to work on pure JVM, without Spring.

## Why not JPA

JPA promises simplicity of usage by providing an object-oriented API. However, this is achieved by
creating a *virtual object database* layer over a relational database; that creates much complexity
under the hood which leaks in various ways. In short, JPA is a double failure: it chose the wrong abstraction,
and implemented it poorly.

There are major issues in JPA which cannot be overlooked:

* [Vaadin-on-Kotlin Issue #3 Remove JPA](https://github.com/mvysny/vaadin-on-kotlin/issues/3)
* [Back to Base - make SQL great again](http://mavi.logdown.com/posts/5771422)
* [Do-It-Yourself ORM as an Alternative to Hibernate](https://blog.philipphauer.de/do-it-yourself-orm-alternative-hibernate-drawbacks/)

We strive to erase the virtual object database layer. We acknowledge the existence of
the relational database; we only provide tools to ease the use of the database from a
statically-typed OOP language.

# Developing jdbi-orm

See [CONTRIBUTING](CONTRIBUTING.md)

# License

Licensed under the [MIT License](https://opensource.org/licenses/MIT).

Copyright (c) 2017-2018 Martin Vysny

All rights reserved.

Permission is hereby granted, free  of charge, to any person obtaining
a  copy  of this  software  and  associated  documentation files  (the
"Software"), to  deal in  the Software without  restriction, including
without limitation  the rights to  use, copy, modify,  merge, publish,
distribute,  sublicense, and/or sell  copies of  the Software,  and to
permit persons to whom the Software  is furnished to do so, subject to
the following conditions:

The  above  copyright  notice  and  this permission  notice  shall  be
included in all copies or substantial portions of the Software.
THE  SOFTWARE IS  PROVIDED  "AS  IS", WITHOUT  WARRANTY  OF ANY  KIND,
EXPRESS OR  IMPLIED, INCLUDING  BUT NOT LIMITED  TO THE  WARRANTIES OF
MERCHANTABILITY,    FITNESS    FOR    A   PARTICULAR    PURPOSE    AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
OF CONTRACT, TORT OR OTHERWISE,  ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
