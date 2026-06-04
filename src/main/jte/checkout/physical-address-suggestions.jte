@import me.noynto.eosa.infrastructure.fetch.photon.resource.PhotonApiResource
@import java.util.List
@param List<PhotonApiResource.Response.Feature> features

@if(!features.isEmpty())
    @for(var feature : features)
        !{var props = feature.properties();}
        !{var coords = feature.geometry().coordinates();}
        !{var lon = coords.get(0);}
        !{var lat = coords.get(1);}
        !{var street = props.name() != null ? props.name() : "";}
        !{var city = props.city() != null ? props.city() : (props.district() != null ? props.district() : "");}
        !{var postcode = props.postcode() != null ? props.postcode() : "";}
        !{var country = props.country() != null ? props.country() : "";}

        <form method="POST" action="/checkout/physical-address">
            <input type="hidden" name="street" value="${street}"/>
            <input type="hidden" name="postcode" value="${postcode}"/>
            <input type="hidden" name="city" value="${city}"/>
            <input type="hidden" name="lat" value="${lat}"/>
            <input type="hidden" name="lon" value="${lon}"/>
            <button type="submit" class="w-full text-left border border-bg-border hover:border-primary bg-bg-soft rounded-2xl overflow-hidden transition-colors group flex items-stretch h-24">
                <div
                    data-map
                    data-lat="${lat}"
                    data-lon="${lon}"
                    class="w-28 flex-shrink-0 bg-bg-border"
                ></div>
                <div class="flex-1 px-4 py-3 flex flex-col justify-center min-w-0">
                    <p class="text-sm font-medium text-primary leading-snug truncate">${street}</p>
                    <p class="text-xs text-secondary mt-0.5 truncate">${postcode} ${city}</p>
                    @if(!country.isEmpty())
                        <p class="text-[10px] text-muted mt-1 truncate">${country}</p>
                    @endif
                </div>
                <div class="flex items-center pr-4 flex-shrink-0">
                    <svg class="text-muted group-hover:text-primary transition-colors" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M5 12h14M12 5l7 7-7 7"/>
                    </svg>
                </div>
            </button>
        </form>
    @endfor
@endif