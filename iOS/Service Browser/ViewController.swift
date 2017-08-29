//
//  ViewController.swift
//  Service Browser
//
//  Created by Christopher Brind on 28/08/2017.
//  Copyright © 2017 Christopher Brind. All rights reserved.
//

import UIKit
import CFNetwork

class ViewController: UITableViewController {

    var browser: NetServiceBrowser!

    var services = [NetService]()
    var serviceDelegates = [NetService: NetServiceDelegate]()

    override func viewDidLoad() {
        super.viewDidLoad()

        refreshControl = UIRefreshControl()
        refreshControl?.addTarget(self, action: #selector(refresh), for: .valueChanged)

        browser = NetServiceBrowser()
        browser.delegate = self
        refresh()
    }

    override func numberOfSections(in tableView: UITableView) -> Int {
        return 1
    }

    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return services.count
    }

    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let service = services[indexPath.row]

        let cell = tableView.dequeueReusableCell(withIdentifier: "Cell")!
        cell.textLabel?.text = service.name
        cell.detailTextLabel?.text = "\(service.hostName ?? ""):\(service.port)"
        return cell
    }

    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {

        let service = services[indexPath.row]

        var inputStream: InputStream?
        var outputStream: OutputStream?

        guard service.getInputStream(&inputStream, outputStream: &outputStream) else {
            print("getInputStream failed")
            return
        }

        guard let input = inputStream else {
            print("no input stream")
            return
        }

        guard let output = outputStream else {
            print("no output stream")
            return
        }

        input.open()
        output.open()

        let written = output.write([ 0, 0x54, 1 ], maxLength: 3)
        guard written > -1 else {
            print("output.write failed", written, output.streamError)
            return
        }

        var buffer:[UInt8] = Array<UInt8>(repeatElement(0, count: 3))
        let read = input.read(&buffer, maxLength: 3)
        guard read > -1 else {
            print("input.read failed", read, input.streamError)
            return
        }

        print("response: ", buffer)
    }

    deinit {
        browser.stop()
        browser = nil
    }

    func refresh() {
        services = [NetService]()
        browser.searchForServices(ofType: "_spike._tcp", inDomain: "")
    }

}

extension ViewController: NetServiceBrowserDelegate {

    func netServiceBrowserWillSearch(_ browser: NetServiceBrowser) {
        print("netServiceBrowserWillSearch")
    }

    func netServiceBrowser(_ browser: NetServiceBrowser, didFindDomain domainString: String, moreComing: Bool) {
        print("netServiceBrowser:didFindDomain", domainString, "moreComing:", moreComing)
    }

    func netServiceBrowser(_ browser: NetServiceBrowser, didNotSearch errorDict: [String : NSNumber]) {
        refreshControl?.endRefreshing()
        print("netServiceBrowser:didNotSearch", errorDict)
    }

    func netServiceBrowser(_ browser: NetServiceBrowser, didFind service: NetService, moreComing: Bool) {
        print("netServiceBrowser:didFind", service, "moreComing:", moreComing)

        print("service.name", service.name)
        print("service.hostName", service.hostName ?? "<unknown host>")
        print("service.port", service.port)
        print("service.includesPeerToPeer", service.includesPeerToPeer)

        services.append(service)
        tableView.reloadData()

        if !moreComing {
            refreshControl?.endRefreshing()
        }
    }

    func netServiceBrowser(_ browser: NetServiceBrowser, didRemove service: NetService, moreComing: Bool) {
        print("netServiceBrowser:didRemove", service, "moreComing:", moreComing)
        if let index = services.index(of: service) {
            services.remove(at: index)
        }
        serviceDelegates[service] = nil
        tableView.reloadData()
    }

    func netServiceBrowserDidStopSearch(_ browser: NetServiceBrowser) {
        print("netServiceBrowserDidStopSearch")
    }

    func netServiceBrowser(_ browser: NetServiceBrowser, didRemoveDomain domainString: String, moreComing: Bool) {
        print("netServiceBrowser:didRemoveDomain", domainString, "moreComing:", moreComing)
    }

}

