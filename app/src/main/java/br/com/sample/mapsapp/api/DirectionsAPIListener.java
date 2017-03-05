package br.com.sample.mapsapp.api;

import java.util.List;
import br.com.sample.mapsapp.models.Route;

public interface DirectionsAPIListener {

    void onDirectionStart();
    void onDirectionSuccess(List<Route> route);

}
