package net.adamcin.jenkins.granite;

import com.ning.http.client.AsyncHttpClient;

import java.io.Serializable;

public interface AHCFactory extends Serializable {
    AsyncHttpClient newInstance();
}
