# Protocol Documentation
<a name="top"></a>

## Table of Contents

- [urlfrontier.proto](#urlfrontier.proto)
    - [BlockQueueParams](#urlfrontier.BlockQueueParams)
    - [Boolean](#urlfrontier.Boolean)
    - [DiscoveredURLItem](#urlfrontier.DiscoveredURLItem)
    - [Empty](#urlfrontier.Empty)
    - [GetParams](#urlfrontier.GetParams)
    - [Integer](#urlfrontier.Integer)
    - [KnownURLItem](#urlfrontier.KnownURLItem)
    - [Pagination](#urlfrontier.Pagination)
    - [QueueDelayParams](#urlfrontier.QueueDelayParams)
    - [QueueList](#urlfrontier.QueueList)
    - [Stats](#urlfrontier.Stats)
    - [Stats.CountsEntry](#urlfrontier.Stats.CountsEntry)
    - [String](#urlfrontier.String)
    - [StringList](#urlfrontier.StringList)
    - [URLInfo](#urlfrontier.URLInfo)
    - [URLInfo.MetadataEntry](#urlfrontier.URLInfo.MetadataEntry)
    - [URLItem](#urlfrontier.URLItem)
  
    - [URLFrontier](#urlfrontier.URLFrontier)
  
- [Scalar Value Types](#scalar-value-types)



<a name="urlfrontier.proto"></a>
<p align="right"><a href="#top">Top</a></p>

## urlfrontier.proto



<a name="urlfrontier.BlockQueueParams"></a>

### BlockQueueParams
Parameter message for BlockQueueUntil *


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| key | [string](#string) |  | ID for the queue * |
| time | [uint64](#uint64) |  | Expressed in seconds of UTC time since Unix epoch 1970-01-01T00:00:00Z. The default value of 0 will unblock the queue. |






<a name="urlfrontier.Boolean"></a>

### Boolean



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| state | [bool](#bool) |  |  |






<a name="urlfrontier.DiscoveredURLItem"></a>

### DiscoveredURLItem
URL discovered during the crawl, might already be known in the URL Frontier or not.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| info | [URLInfo](#urlfrontier.URLInfo) |  |  |






<a name="urlfrontier.Empty"></a>

### Empty







<a name="urlfrontier.GetParams"></a>

### GetParams
Parameter message for GetURLs *


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| max_urls_per_queue | [uint32](#uint32) |  | maximum number of URLs per queue, the default value of 0 means no limit |
| max_queues | [uint32](#uint32) |  | maximum number of queues to get URLs from, the default value of 0 means no limit |
| key | [string](#string) |  | queue id if restricting to a specific queue |
| delay_requestable | [uint32](#uint32) |  | delay in seconds before a URL can be unlocked and sent again for fetching |






<a name="urlfrontier.Integer"></a>

### Integer



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| value | [uint64](#uint64) |  |  |






<a name="urlfrontier.KnownURLItem"></a>

### KnownURLItem
URL which was already known in the frontier, was returned by GetURLs() and processed by the crawler. Used for updating the information 
about it in the frontier. If the date is not set, the URL will be considered done and won&#39;t be resubmitted for fetching, otherwise
it will be elligible for fetching after the delay has elapsed.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| info | [URLInfo](#urlfrontier.URLInfo) |  |  |
| refetchable_from_date | [uint64](#uint64) |  | Expressed in seconds of UTC time since Unix epoch 1970-01-01T00:00:00Z. Optional, the default value of 0 indicates that a URL should not be refetched. |






<a name="urlfrontier.Pagination"></a>

### Pagination



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| start | [uint32](#uint32) |  | position of the first result in the list; defaults to 0 |
| size | [uint32](#uint32) |  | max number of values; defaults to 100 |






<a name="urlfrontier.QueueDelayParams"></a>

### QueueDelayParams
Parameter message for SetDelay *


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| key | [string](#string) |  | ID for the queue - an empty value sets the default for all the queues * |
| delay_requestable | [uint32](#uint32) |  | delay in seconds before a queue can provide new URLs |






<a name="urlfrontier.QueueList"></a>

### QueueList
Returned by ListQueues *


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| values | [string](#string) | repeated |  |
| total | [uint64](#uint64) |  | total number of queues |
| start | [uint32](#uint32) |  | position of the first result in the list |
| size | [uint32](#uint32) |  | number of values returned |






<a name="urlfrontier.Stats"></a>

### Stats
Message returned by the GetStats method


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| size | [uint64](#uint64) |  | number of active URLs in queues |
| inProcess | [uint32](#uint32) |  | number of URLs currently in flight |
| counts | [Stats.CountsEntry](#urlfrontier.Stats.CountsEntry) | repeated | custom counts |
| numberOfQueues | [uint64](#uint64) |  | number of active queues in the frontier |






<a name="urlfrontier.Stats.CountsEntry"></a>

### Stats.CountsEntry



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| key | [string](#string) |  |  |
| value | [uint64](#uint64) |  |  |






<a name="urlfrontier.String"></a>

### String



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| value | [string](#string) |  |  |






<a name="urlfrontier.StringList"></a>

### StringList



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| values | [string](#string) | repeated |  |






<a name="urlfrontier.URLInfo"></a>

### URLInfo



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| url | [string](#string) |  | URL * |
| key | [string](#string) |  | The key is used to put the URLs into queues, the value can be anything set by the client but would typically be the hostname, domain name or IP or the URL. If not set, the service will use a sensible default like hostname. |
| metadata | [URLInfo.MetadataEntry](#urlfrontier.URLInfo.MetadataEntry) | repeated | Arbitrary key / values stored alongside the URL. Can be anything needed by the crawler like http status, source URL etc... |






<a name="urlfrontier.URLInfo.MetadataEntry"></a>

### URLInfo.MetadataEntry



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| key | [string](#string) |  |  |
| value | [StringList](#urlfrontier.StringList) |  |  |






<a name="urlfrontier.URLItem"></a>

### URLItem
Wrapper for a KnownURLItem or DiscoveredURLItem *


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| discovered | [DiscoveredURLItem](#urlfrontier.DiscoveredURLItem) |  |  |
| known | [KnownURLItem](#urlfrontier.KnownURLItem) |  |  |





 

 

 


<a name="urlfrontier.URLFrontier"></a>

### URLFrontier


| Method Name | Request Type | Response Type | Description |
| ----------- | ------------ | ------------- | ------------|
| ListQueues | [Pagination](#urlfrontier.Pagination) | [QueueList](#urlfrontier.QueueList) | Return the names of up to N active queues a queue is active if it has URLs due for fetching; by default the service will return up to 100 results from offset 0 * |
| GetURLs | [GetParams](#urlfrontier.GetParams) | [URLInfo](#urlfrontier.URLInfo) stream | Stream URLs due for fetching from M queues with up to N items per queue * |
| PutURLs | [URLItem](#urlfrontier.URLItem) stream | [String](#urlfrontier.String) stream | Push URL items to the server; they get created (if they don&#39;t already exist) in case of DiscoveredURLItems or updated if KnownURLItems * |
| GetStats | [String](#urlfrontier.String) | [Stats](#urlfrontier.Stats) | Return stats for a specific queue or the whole crawl if the value if empty or null * |
| DeleteQueue | [String](#urlfrontier.String) | [Integer](#urlfrontier.Integer) | Delete the queue based on the key in parameter, returns the number of URLs removed this way * |
| BlockQueueUntil | [BlockQueueParams](#urlfrontier.BlockQueueParams) | [Empty](#urlfrontier.Empty) | Block a queue from sending URLs; the argument is the number of seconds of UTC time since Unix epoch 1970-01-01T00:00:00Z. The default value of 0 will unblock the queue. The block will get removed once the time indicated in argument is reached. This is useful for cases where a server returns a Retry-After for instance. |
| SetActive | [Boolean](#urlfrontier.Boolean) | [Empty](#urlfrontier.Empty) | De/activate the crawl. GetURLs will not return anything until SetActive is set to true. PutURLs will still take incoming data. * |
| GetActive | [Empty](#urlfrontier.Empty) | [Boolean](#urlfrontier.Boolean) | Returns true if the crawl is active, false if it has been deactivated with SetActive(Boolean) * |
| SetDelay | [QueueDelayParams](#urlfrontier.QueueDelayParams) | [Empty](#urlfrontier.Empty) | Set a delay from a given queue. No URLs will be obtained via GetURLs for this queue until the number of seconds specified has elapsed since the last time URLs were retrieved. Usually informed by the delay setting of robots.txt. |

 



## Scalar Value Types

| .proto Type | Notes | C++ | Java | Python | Go | C# | PHP | Ruby |
| ----------- | ----- | --- | ---- | ------ | -- | -- | --- | ---- |
| <a name="double" /> double |  | double | double | float | float64 | double | float | Float |
| <a name="float" /> float |  | float | float | float | float32 | float | float | Float |
| <a name="int32" /> int32 | Uses variable-length encoding. Inefficient for encoding negative numbers – if your field is likely to have negative values, use sint32 instead. | int32 | int | int | int32 | int | integer | Bignum or Fixnum (as required) |
| <a name="int64" /> int64 | Uses variable-length encoding. Inefficient for encoding negative numbers – if your field is likely to have negative values, use sint64 instead. | int64 | long | int/long | int64 | long | integer/string | Bignum |
| <a name="uint32" /> uint32 | Uses variable-length encoding. | uint32 | int | int/long | uint32 | uint | integer | Bignum or Fixnum (as required) |
| <a name="uint64" /> uint64 | Uses variable-length encoding. | uint64 | long | int/long | uint64 | ulong | integer/string | Bignum or Fixnum (as required) |
| <a name="sint32" /> sint32 | Uses variable-length encoding. Signed int value. These more efficiently encode negative numbers than regular int32s. | int32 | int | int | int32 | int | integer | Bignum or Fixnum (as required) |
| <a name="sint64" /> sint64 | Uses variable-length encoding. Signed int value. These more efficiently encode negative numbers than regular int64s. | int64 | long | int/long | int64 | long | integer/string | Bignum |
| <a name="fixed32" /> fixed32 | Always four bytes. More efficient than uint32 if values are often greater than 2^28. | uint32 | int | int | uint32 | uint | integer | Bignum or Fixnum (as required) |
| <a name="fixed64" /> fixed64 | Always eight bytes. More efficient than uint64 if values are often greater than 2^56. | uint64 | long | int/long | uint64 | ulong | integer/string | Bignum |
| <a name="sfixed32" /> sfixed32 | Always four bytes. | int32 | int | int | int32 | int | integer | Bignum or Fixnum (as required) |
| <a name="sfixed64" /> sfixed64 | Always eight bytes. | int64 | long | int/long | int64 | long | integer/string | Bignum |
| <a name="bool" /> bool |  | bool | boolean | boolean | bool | bool | boolean | TrueClass/FalseClass |
| <a name="string" /> string | A string must always contain UTF-8 encoded or 7-bit ASCII text. | string | String | str/unicode | string | string | string | String (UTF-8) |
| <a name="bytes" /> bytes | May contain any arbitrary sequence of bytes. | string | ByteString | str | []byte | ByteString | string | String (ASCII-8BIT) |

