export interface ContainerListResponse {
  Id: string
  Names: string[]
  Image: string
  ImageID: string
  Command: string
  Created: number
  State: string
  Status: string
  Ports: {
    IP: string
    PrivatePort: number
    PublicPort: number
    Type: string
  }[]
  Labels: Record<string, string>
  NetworkSettings: {
    Networks: Record<string, {
      IPAddress: string
      Gateway: string
      NetworkID: string
    }>
  }
  Mounts: {
    Type: string
    Source: string
    Destination: string
    Mode: string
    RW: boolean
  }[]
} 