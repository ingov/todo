package de.spiegel.timeregistration.business.timeentries.boundary;

import com.airhacks.rulz.jaxrsclient.JAXRSClientProvider;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.junit.Rule;
import org.junit.Test;

/**
 *
 * @author veithi
 */
public class TimeEntriesResourcesIT {

    @Rule
    public JAXRSClientProvider provider = JAXRSClientProvider.buildWithURI("http://localhost:8080/timeRegistration/api/timeentries");

    @Test
    public void crud() {
        JsonObjectBuilder timeEntryBuilder = Json.createObjectBuilder();
        JsonObject timeEntryJson = timeEntryBuilder
                .add("caption", "implement 42")
                .add("description", "...")
                .add("priority", 42)
                .build();

        Response postReponse = this.provider.target().request().post(Entity.json(timeEntryJson));
        assertThat(postReponse.getStatus(), is(201));
        String location = postReponse.getHeaderString("Location");
        System.out.println("loaction = " + location);

        // find all
        Response response = this.provider.target()
                .request(MediaType.APPLICATION_JSON)
                .get();
        assertThat(response.getStatus(), is(200));
        JsonArray allTimeEntries = response.readEntity(JsonArray.class);
        assertFalse(allTimeEntries.isEmpty());

        // find
        JsonObject dedicatedTimeEntry = this.provider.client()
                .target(location)
                .request(MediaType.APPLICATION_JSON)
                .get(JsonObject.class);
        assertTrue(dedicatedTimeEntry.getString("caption").contains("42"));
        long version = dedicatedTimeEntry.getJsonNumber("version").longValue();

        // update
        JsonObjectBuilder updateBuilder = Json.createObjectBuilder();
        JsonObject updated = updateBuilder
                .add("caption", "implement 42 updated")
                .add("version", version)
                .build();

        Response updateResponse = this.provider.client().target(location)
                .request(MediaType.APPLICATION_JSON)
                .put(Entity.json(updated));
        assertThat(updateResponse.getStatus(), is(200));

        // update again
        updateBuilder = Json.createObjectBuilder();
        updated = updateBuilder
                .add("caption", "implement 42 updated 2")
                .add("priority", 12)
                .build();

        updateResponse = this.provider.client().target(location)
                .request(MediaType.APPLICATION_JSON)
                .put(Entity.json(updated));
        assertThat(updateResponse.getStatus(), is(200));

        //find again
        JsonObject updatedTimeEntry = this.provider.client()
                .target(location)
                .request(MediaType.APPLICATION_JSON)
                .get(JsonObject.class);
        assertTrue(updatedTimeEntry.getString("caption").contains("update"));

        // update status
        JsonObjectBuilder statusBuilder = Json.createObjectBuilder();
        JsonObject status = updateBuilder
                .add("done", true)
                .build();

        Response statusResponse = this.provider.client().target(location)
                .path("status")
                .request(MediaType.APPLICATION_JSON)
                .put(Entity.json(status));

        //verify status
        updatedTimeEntry = this.provider.client()
                .target(location)
                .request(MediaType.APPLICATION_JSON)
                .get(JsonObject.class);
        assertTrue(updatedTimeEntry.getBoolean("done"));

        // update not existing status
        JsonObjectBuilder notexistingBuilder = Json.createObjectBuilder();
        status = updateBuilder
                .add("done", true)
                .build();

        statusResponse = this.provider.target()
                .path("-42")
                .path("status")
                .request(MediaType.APPLICATION_JSON)
                .put(Entity.json(status));
        assertThat(statusResponse.getStatus(), is(400));
        assertFalse(statusResponse.getHeaderString("reason").isEmpty());

        // update malformed status
        JsonObjectBuilder malformedBuilder = Json.createObjectBuilder();
        status = updateBuilder
                .add("womething wrong", true)
                .build();

        statusResponse = this.provider.client()
                .target(location)
                .path("status")
                .request(MediaType.APPLICATION_JSON)
                .put(Entity.json(status));
        assertThat(statusResponse.getStatus(), is(400));
        assertFalse(statusResponse.getHeaderString("reason").isEmpty());

//      delete non-existing
        Response deleteResponse = this.provider.target()
                .path("42")
                .request(MediaType.APPLICATION_JSON)
                .delete();
        System.out.println("deleteResponse = " + deleteResponse);
        assertThat(deleteResponse.getStatus(), is(204));
    }

}
