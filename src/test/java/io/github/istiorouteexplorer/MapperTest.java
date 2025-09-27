package io.github.istiorouteexplorer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.fabric8.istio.api.api.networking.v1alpha3.Destination;
import io.fabric8.istio.api.api.networking.v1alpha3.HTTPRoute;
import io.fabric8.istio.api.api.networking.v1alpha3.HTTPRouteDestination;
import io.fabric8.istio.api.networking.v1beta1.VirtualService;
import io.fabric8.istio.api.api.networking.v1alpha3.PortSelector;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.github.istiorouteexplorer.config.KubernetesClientConfig;
import io.github.istiorouteexplorer.model.istio.HttpRouteDestinationDto;
import io.github.istiorouteexplorer.model.istio.HttpRouteDto;
import io.github.istiorouteexplorer.model.istio.VirtualServiceDto;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

class MapperTest {

    @Test
    void mapsFabric8VirtualServiceToDto() {
        ModelMapper mapper = new KubernetesClientConfig().modelMapper();

        VirtualService virtualService = new VirtualService();
        ObjectMeta metadata = new ObjectMeta();
        metadata.setLabels(new java.util.HashMap<>());
        metadata.setName("reviews");
        metadata.setNamespace("bookinfo");
        metadata.getLabels().put("app", "reviews");
        virtualService.setMetadata(metadata);

        Destination destination = new Destination();
        destination.setHost("ratings");
        destination.setSubset("v1");
        PortSelector portSelector = new PortSelector();
        portSelector.setNumber(9080L);
        destination.setPort(portSelector);

        HTTPRouteDestination primaryRoute = new HTTPRouteDestination();
        primaryRoute.setDestination(destination);
        primaryRoute.setWeight(80);

        HTTPRoute httpRoute = new HTTPRoute();
        httpRoute.setRoute(List.of(primaryRoute));

        io.fabric8.istio.api.api.networking.v1alpha3.VirtualService spec = new io.fabric8.istio.api.api.networking.v1alpha3.VirtualService();
        spec.setHosts(List.of("reviews"));
        spec.setHttp(List.of(httpRoute));
        virtualService.setSpec(spec);

        VirtualServiceDto dto = mapper.map(virtualService, VirtualServiceDto.class);

        assertNotNull(dto);
        assertEquals("reviews", dto.metadata().name());
        assertEquals("bookinfo", dto.metadata().namespace());
        assertEquals(List.of("reviews"), dto.spec().hosts());

        List<HttpRouteDto> httpRoutes = dto.spec().http();
        assertNotNull(httpRoutes);
        assertEquals(1, httpRoutes.size());
        HttpRouteDto mappedRoute = httpRoutes.get(0);
        assertNull(mappedRoute.mirror());
        List<HttpRouteDestinationDto> mappedDestinations = mappedRoute.route();
        assertNotNull(mappedDestinations);
        assertEquals(1, mappedDestinations.size());
        HttpRouteDestinationDto mappedDestination = mappedDestinations.get(0);
        assertEquals("ratings", mappedDestination.destination().host());
        assertEquals(9080L, mappedDestination.destination().port());
        assertEquals("v1", mappedDestination.destination().subset());
        assertEquals(80, mappedDestination.weight());
    }
}
