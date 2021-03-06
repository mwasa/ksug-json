package org.ksug.json.test.jsonp;

import static org.junit.jupiter.api.Assertions.*;
import static org.ksug.json.test.JsonUtils.*;

import java.util.Collections;
import java.util.List;

import javax.json.Json;
import javax.json.JsonMergePatch;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ksug.json.fixture.Project;
import org.ksug.json.fixture.User;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

/**
 * Created by mhyeon.lee on 2017. 11. 19..
 */
@DisplayName("Json Merge Patch Test")
class JsonMergePatchTest {
	private Jsonb jsonb;

	private Project object;

	@BeforeEach
	void setUp() {
		this.jsonb = JsonbBuilder.create();
		this.object = Project.builder()
				.name("json-merge-patch")
				.description("json merge patch")
				.policy("public")
				.tags(Collections.singletonList("json-tags"))
				.creator(User.builder()
						.name("user")
						.age(30)
						.build())
				.build();
	}

	@Test
	void jsonMergePatch() {
		// Given
		JsonObject source = objectToJsonObject(this.jsonb, this.object);

		JsonObject mergePatch = jsonToJsonObject(
				this.jsonb,
				"{\"name\":\"patched\", \"policy\":null, \"creator\":{\"name\":null}}");
		JsonMergePatch sut = Json.createMergePatch(mergePatch);

		// When
		JsonValue result = sut.apply(source);
		String expected = result.asJsonObject().toString();

		// Then
		System.out.println(expected);
		DocumentContext document = JsonPath.parse(expected);
		assertEquals("patched", document.read("$.name", String.class));
		assertEquals(this.object.getDescription(), document.read("$.description", String.class));
		assertThrows(PathNotFoundException.class, () -> document.read("$.policy"));
		assertEquals(this.object.getTags(), document.read("$.tags", List.class));
		assertThrows(PathNotFoundException.class, () -> document.read("$.creator.name"));
		assertSame(this.object.getCreator().getAge(), document.read("$.creator.age", Integer.class));

		Project project = this.jsonb.fromJson(expected, Project.class);
		assertEquals("patched", project.getName());
		assertEquals(this.object.getDescription(), project.getDescription());
		assertNull(project.getPolicy());
		assertEquals(this.object.getTags(), project.getTags());
		assertNull(project.getCreator().getName());
		assertSame(this.object.getCreator().getAge(), project.getCreator().getAge());
	}

	@Test
	void diff() {
		// Given
		JsonObject source = objectToJsonObject(this.jsonb, this.object);
		JsonObject target = jsonToJsonObject(
				this.jsonb,
				"{\"name\":\"target\", \"policy\":\"private\", \"creator\":{\"age\":40}}");

		// When
		JsonMergePatch expected = Json.createMergeDiff(source, target);

		// Then
		String resultJson = expected.toJsonValue().toString();
		System.out.println(resultJson);

		DocumentContext document = JsonPath.parse(resultJson);
		assertEquals("target", document.read("$.name", String.class));
		assertNull(document.read("$.description"));
		assertEquals("private", document.read("$.policy", String.class));
		assertNull(document.read("$.tags", List.class));
		assertNull(document.read("$.creator.name"));
		assertEquals(40, document.read("$.creator.age", Integer.class).intValue());

		JsonValue patched = expected.apply(source);
		Project project = this.jsonb.fromJson(patched.toString(), Project.class);

		assertEquals("target", project.getName());
		assertNull(project.getDescription());
		assertEquals("private", project.getPolicy());
		assertEquals(Collections.emptyList(), project.getTags());
		assertNull(project.getCreator().getName());
		assertSame(40, project.getCreator().getAge());
	}
}
