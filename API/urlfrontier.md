# Protocol Documentation
<a name="top"></a>

## Table of Contents

- [urlfrontier.proto](#urlfrontier-proto)
    - [AckMessage](#urlfrontier-AckMessage)
    - [Active](#urlfrontier-Active)
    - [AnyCrawlID](#urlfrontier-AnyCrawlID)
    - [BlockQueueParams](#urlfrontier-BlockQueueParams)
    - [Boolean](#urlfrontier-Boolean)
    - [CountUrlParams](#urlfrontier-CountUrlParams)
    - [CrawlLimitParams](#urlfrontier-CrawlLimitParams)
    - [DeleteCrawlMessage](#urlfrontier-DeleteCrawlMessage)
    - [DiscoveredURLItem](#urlfrontier-DiscoveredURLItem)
    - [Empty](#urlfrontier-Empty)
    - [GetParams](#urlfrontier-GetParams)
    - [KnownURLItem](#urlfrontier-KnownURLItem)
    - [ListUrlParams](#urlfrontier-ListUrlParams)
    - [Local](#urlfrontier-Local)
    - [LogLevelParams](#urlfrontier-LogLevelParams)
    - [Long](#urlfrontier-Long)
    - [Pagination](#urlfrontier-Pagination)
    - [QueueDelayParams](#urlfrontier-QueueDelayParams)
    - [QueueList](#urlfrontier-QueueList)
    - [QueueWithinCrawlParams](#urlfrontier-QueueWithinCrawlParams)
    - [Stats](#urlfrontier-Stats)
    - [Stats.CountsEntry](#urlfrontier-Stats-CountsEntry)
    - [StringList](#urlfrontier-StringList)
    - [URLInfo](#urlfrontier-URLInfo)
    - [URLInfo.MetadataEntry](#urlfrontier-URLInfo-MetadataEntry)
    - [URLItem](#urlfrontier-URLItem)
    - [URLStatusRequest](#urlfrontier-URLStatusRequest)
  
    - [AckMessage.Status](#urlfrontier-AckMessage-Status)
    - [LogLevelParams.Level](#urlfrontier-LogLevelParams-Level)
  
    - [URLFrontier](#urlfrontier-URLFrontier)
  
- [Scalar Value Types](#scalar-value-types)



<a name="urlfrontier-proto"></a>
<p align="right"><a href="#top">Top</a></p>

## urlfrontier.proto



<a name="urlfrontier-AckMessage"></a>

### AckMessage



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| ID | [string](#string) |  | ID which had been specified by the client * |
| status | [AckMessage.Status](#urlfrontier-AckMessage-Status) |  |  |






<a name="urlfrontier-Active"></a>

### Active



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| state | [bool](#bool) |  |  |
| local | [bool](#bool) |  |  |






<a name="urlfrontier-AnyCrawlID"></a>

### AnyCrawlID







<a name="urlfrontier-BlockQueueParams"></a>

### BlockQueueParams
Parameter message for BlockQueueUntil *


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| key | [string](#string) |  | ID for the queue * |
| time | [uint64](#uint64) |  | Expressed in seconds of UTC time since Unix epoch 1970-01-01T00:00:00Z. The default value of 0 will unblock the queue. |
| crawlID | [string](#string) |  | crawl ID |
| local | [bool](#bool) |  | only for this instance |






<a name="urlfrontier-Boolean"></a>

### Boolean



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| state | [bool](#bool) |  |  |






<a name="urlfrontier-CountUrlParams"></a>

### CountUrlParams



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| key | [string](#string) |  | ID for the queue * |
| crawlID | [string](#string) |  | crawl ID |
| filter | [string](#string) | optional | Search filter on url (can be empty, default is empty) |
| ignoreCase | [bool](#bool) | optional | Ignore Case sensitivity for search filter (default is false -&gt; case sensitive) |
| local | [bool](#bool) | optional | only for the current local instance (default is false) |






<a name="urlfrontier-CrawlLimitParams"></a>

### CrawlLimitParams
Parameter message for SetCrawlLimit *


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| key | [string](#string) |  | ID for the queue * |
| limit | [uint32](#uint32) |  |  |
| crawlID | [string](#string) |  | crawl ID |






<a name="urlfrontier-DeleteCrawlMessage"></a>

### DeleteCrawlMessage



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| value | [string](#string) |  |  |
| local | [bool](#bool) |  |  |






<a name="urlfrontier-DiscoveredURLItem"></a>

### DiscoveredURLItem

URL discovered during the crawl, might already be known in the URL Frontier or not.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| info | [URLInfo](#urlfrontier-URLInfo) |  |  |






<a name="urlfrontier-Empty"></a>

### Empty







<a name="urlfrontier-GetParams"></a>

### GetParams
Parameter message for GetURLs *


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| max_urls_per_queue | [uint32](#uint32) |  | maximum number of URLs per queue, the default value of 0 means no limit |
| max_queues | [uint32](#uint32) |  | maximum number of queues to get URLs from, the default value of 0 means no limit |
| key | [string](#string) |  | queue id if restricting to a specific queue |
| delay_requestable | [uint32](#uint32) |  | delay in seconds before a URL can be unlocked and sent again for fetching |
| anyCrawlID | [AnyCrawlID](#urlfrontier-AnyCrawlID) |  |  |
| crawlID | [string](#string) |  |  |






<a name="urlfrontier-KnownURLItem"></a>

### KnownURLItem

URL which was already known in the frontier, was returned by GetURLs() and processed by the crawler. Used for updating the information 
about it in the frontier. If the date is not set, the URL will be considered done and won&#39;t be resubmitted for fetching, otherwise
it will be elligible for fetching after the delay has elapsed.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| info | [URLInfo](#urlfrontier-URLInfo) |  |  |
| refetchable_from_date | [uint64](#uint64) |  | Expressed in seconds of UTC time since Unix epoch 1970-01-01T00:00:00Z. Optional, the default value of 0 indicates that a URL should not be refetched. |






<a name="urlfrontier-ListUrlParams"></a>

### ListUrlParams



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| start | [uint32](#uint32) |  | position of the first result in the list; defaults to 0 |
| size | [uint32](#uint32) |  | max number of values; defaults to 100 |
| key | [string](#string) |  | ID for the queue * |
| crawlID | [string](#string) |  | crawl ID |
| local | [bool](#bool) |  | only for the current local instance |
| filter | [string](#string) | optional | Search filter on url (can be empty, default is empty) |
| ignoreCase | [bool](#bool) | optional | Ignore Case sensitivity for search filter (default is false -&gt; case sensitive) |






<a name="urlfrontier-Local"></a>

### Local



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| local | [bool](#bool) |  |  |






<a name="urlfrontier-LogLevelParams"></a>

### LogLevelParams

Configuration of the log level for a particular package, e.g.
crawlercommons.urlfrontier.service.rocksdb DEBUG


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| package | [string](#string) |  |  |
| level | [LogLevelParams.Level](#urlfrontier-LogLevelParams-Level) |  |  |
| local | [bool](#bool) |  | only for this instance |






<a name="urlfrontier-Long"></a>

### Long



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| value | [uint64](#uint64) |  |  |






<a name="urlfrontier-Pagination"></a>

### Pagination



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| start | [uint32](#uint32) |  | position of the first result in the list; defaults to 0 |
| size | [uint32](#uint32) |  | max number of values; defaults to 100 |
| include_inactive | [bool](#bool) |  | include inactive queues; defaults to false |
| crawlID | [string](#string) |  | crawl ID |
| local | [bool](#bool) |  | only for the current local instance |






<a name="urlfrontier-QueueDelayParams"></a>

### QueueDelayParams
Parameter message for SetDelay *


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| key | [string](#string) |  | ID for the queue - an empty value sets the default for all the queues * |
| delay_requestable | [uint32](#uint32) |  | delay in seconds before a queue can provide new URLs |
| crawlID | [string](#string) |  | crawl ID - empty string for default |
| local | [bool](#bool) |  | only for this instance |






<a name="urlfrontier-QueueList"></a>

### QueueList
Returned by ListQueues *


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| values | [string](#string) | repeated |  |
| total | [uint64](#uint64) |  | total number of queues |
| start | [uint32](#uint32) |  | position of the first result in the list |
| size | [uint32](#uint32) |  | number of values returned |
| crawlID | [string](#string) |  | crawl ID - empty string for default |






<a name="urlfrontier-QueueWithinCrawlParams"></a>

### QueueWithinCrawlParams



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| key | [string](#string) |  | ID for the queue * |
| crawlID | [string](#string) |  | crawl ID - empty string for default |
| local | [bool](#bool) |  | only for this instance |






<a name="urlfrontier-Stats"></a>

### Stats

Message returned by the GetStats method


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| size | [uint64](#uint64) |  | number of active URLs in queues |
| inProcess | [uint32](#uint32) |  | number of URLs currently in flight |
| counts | [Stats.CountsEntry](#urlfrontier-Stats-CountsEntry) | repeated | custom counts |
| numberOfQueues | [uint64](#uint64) |  | number of active queues in the frontier |
| crawlID | [string](#string) |  | crawl ID |






<a name="urlfrontier-Stats-CountsEntry"></a>

### Stats.CountsEntry



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| key | [string](#string) |  |  |
| value | [uint64](#uint64) |  |  |






<a name="urlfrontier-StringList"></a>

### StringList



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| values | [string](#string) | repeated |  |






<a name="urlfrontier-URLInfo"></a>

### URLInfo



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| url | [string](#string) |  | URL * |
| key | [string](#string) |  | The key is used to put the URLs into queues, the value can be anything set by the client but would typically be the hostname, domain name or IP or the URL. If not set, the service will use a sensible default like hostname. |
| metadata | [URLInfo.MetadataEntry](#urlfrontier-URLInfo-MetadataEntry) | repeated |  Arbitrary key / values stored alongside the URL. Can be anything needed by the crawler like http status, source URL etc... |
| crawlID | [string](#string) |  | crawl ID * |






<a name="urlfrontier-URLInfo-MetadataEntry"></a>

### URLInfo.MetadataEntry



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| key | [string](#string) |  |  |
| value | [StringList](#urlfrontier-StringList) |  |  |






<a name="urlfrontier-URLItem"></a>

### URLItem
Wrapper for a KnownURLItem or DiscoveredURLItem *


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| discovered | [DiscoveredURLItem](#urlfrontier-DiscoveredURLItem) |  |  |
| known | [KnownURLItem](#urlfrontier-KnownURLItem) |  |  |
| ID | [string](#string) |  | Identifier specified by the client, if missing, the URL is returned * |






<a name="urlfrontier-URLStatusRequest"></a>

### URLStatusRequest



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| url | [string](#string) |  | URL for which we request info |
| key | [string](#string) |  | ID for the queue * |
| crawlID | [string](#string) |  | crawl ID - empty string for default |





 


<a name="urlfrontier-AckMessage-Status"></a>

### AckMessage.Status


| Name | Number | Description |
| ---- | ------ | ----------- |
| OK | 0 |  |
| SKIPPED | 1 |  |
| FAIL | 2 |  |



<a name="urlfrontier-LogLevelParams-Level"></a>

### LogLevelParams.Level


| Name | Number | Description |
| ---- | ------ | ----------- |
| TRACE | 0 |  |
| DEBUG | 1 |  |
| INFO | 2 |  |
| WARN | 3 |  |
| ERROR | 4 |  |


 

 


<a name="urlfrontier-URLFrontier"></a>

### URLFrontier


| Method Name | Request Type | Response Type | Description |
| ----------- | ------------ | ------------- | ------------|
| ListNodes | [Empty](#urlfrontier-Empty) | [StringList](#urlfrontier-StringList) | Return the list of nodes forming the cluster the current node belongs to * |
| ListCrawls | [Local](#urlfrontier-Local) | [StringList](#urlfrontier-StringList) | Return the list of crawls handled by the frontier(s) * |
| DeleteCrawl | [DeleteCrawlMessage](#urlfrontier-DeleteCrawlMessage) | [Long](#urlfrontier-Long) | Delete an entire crawl, returns the number of URLs removed this way * |
| ListQueues | [Pagination](#urlfrontier-Pagination) | [QueueList](#urlfrontier-QueueList) | Return a list of queues for a specific crawl. Can chose whether to include inactive queues (a queue is active if it has URLs due for fetching); by default the service will return up to 100 results from offset 0 and exclude inactive queues.* |
| GetURLs | [GetParams](#urlfrontier-GetParams) | [URLInfo](#urlfrontier-URLInfo) stream | Stream URLs due for fetching from M queues with up to N items per queue * |
| PutURLs | [URLItem](#urlfrontier-URLItem) stream | [AckMessage](#urlfrontier-AckMessage) stream | Push URL items to the server; they get created (if they don&#39;t already exist) in case of DiscoveredURLItems or updated if KnownURLItems * |
| GetStats | [QueueWithinCrawlParams](#urlfrontier-QueueWithinCrawlParams) | [Stats](#urlfrontier-Stats) | Return stats for a specific queue or an entire crawl. Does not aggregate the stats across different crawlids. * |
| DeleteQueue | [QueueWithinCrawlParams](#urlfrontier-QueueWithinCrawlParams) | [Long](#urlfrontier-Long) | Delete the queue based on the key in parameter, returns the number of URLs removed this way * |
| BlockQueueUntil | [BlockQueueParams](#urlfrontier-BlockQueueParams) | [Empty](#urlfrontier-Empty) | Block a queue from sending URLs; the argument is the number of seconds of UTC time since Unix epoch 1970-01-01T00:00:00Z. The default value of 0 will unblock the queue. The block will get removed once the time indicated in argument is reached. This is useful for cases where a server returns a Retry-After for instance. |
| SetActive | [Active](#urlfrontier-Active) | [Empty](#urlfrontier-Empty) | De/activate the crawl. GetURLs will not return anything until SetActive is set to true. PutURLs will still take incoming data. * |
| GetActive | [Local](#urlfrontier-Local) | [Boolean](#urlfrontier-Boolean) | Returns true if the crawl is active, false if it has been deactivated with SetActive(Boolean) * |
| SetDelay | [QueueDelayParams](#urlfrontier-QueueDelayParams) | [Empty](#urlfrontier-Empty) | Set a delay from a given queue. No URLs will be obtained via GetURLs for this queue until the number of seconds specified has elapsed since the last time URLs were retrieved. Usually informed by the delay setting of robots.txt. |
| SetLogLevel | [LogLevelParams](#urlfrontier-LogLevelParams) | [Empty](#urlfrontier-Empty) | Overrides the log level for a given package * |
| SetCrawlLimit | [CrawlLimitParams](#urlfrontier-CrawlLimitParams) | [Empty](#urlfrontier-Empty) | Sets crawl limit for domain * |
| GetURLStatus | [URLStatusRequest](#urlfrontier-URLStatusRequest) | [URLItem](#urlfrontier-URLItem) | Get status of a particular URL This does not take into account URL scheduling. Used to check current status of an URL within the frontier |
| ListURLs | [ListUrlParams](#urlfrontier-ListUrlParams) | [URLItem](#urlfrontier-URLItem) stream | List all URLs currently in the frontier This does not take into account URL scheduling. Used to check current status of all URLs within the frontier |
| CountURLs | [CountUrlParams](#urlfrontier-CountUrlParams) | [Long](#urlfrontier-Long) | Count URLs currently in the frontier * |

 



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

