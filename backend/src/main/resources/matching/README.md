# Region centers

`region-centers.csv` contains area-weighted centroids derived from KOSTAT 2018 administrative boundaries published by southkorea/southkorea-maps:
https://github.com/southkorea/southkorea-maps/blob/master/kostat/2018/json/skorea-municipalities-2018-geo.json

Municipal wards are aggregated to the city level where the profile selector uses a city. Incheon Nam-gu is labeled Michuhol-gu. All existing profile region options are covered. Distances are approximate straight-line distances between these administrative-area centroids, not users' GPS positions or travel distances. The UI explicitly discloses this basis. Unknown regions do not pass a radius filter; unrestricted matching continues to work.

Keep this attribution with the derived dataset. Source repository terms: https://github.com/southkorea/southkorea-maps/tree/master/license
