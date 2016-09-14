package observo.utils;

import org.junit.Test;

import java.io.IOException;
import java.io.Serializable;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class SerializerTest {

    @Test
    public void canSerializeAndDeserializeAString() throws IOException, ClassNotFoundException {
        String something = "test";
        byte[] serialized = Serializer.serialize(something);
        assertThat(Serializer.deserialize(serialized, String.class), is(something));
    }

    @Test
    public void canSerializeAndDeserializeNull() throws IOException, ClassNotFoundException {
        byte[] serialized = Serializer.serialize(null);
        assertThat(Serializer.deserialize(serialized, String.class), is(nullValue()));
    }

    @Test
    public void canSerializeAndDeserializeAnObject() throws IOException, ClassNotFoundException {
        Person john = new Person("John", 34);
        byte[] serialized = Serializer.serialize(john);
        assertThat(Serializer.deserialize(serialized, Person.class), is(john));
    }

    private static class Person implements Serializable {
        private final String name;
        private final int age;

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public String toString() {
            return "Person{" +
                    "name='" + name + '\'' +
                    ", age=" + age +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Person)) return false;

            Person person = (Person) o;

            if (age != person.age) return false;
            return name != null ? name.equals(person.name) : person.name == null;

        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + age;
            return result;
        }
    }
}